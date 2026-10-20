package com.FoodtimeNeo.media.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.UUID;

/** Private staging files. Publication follows the existing catalog/image references and statuses. */
@Service
public class ImageStorageService {
    public record UploadedImage(UUID id, String imageUrl, int width, int height) { }
    public record StoredImage(byte[] bytes, String contentType, boolean cacheable, long lastModified) { }
    private final Path directory;
    private final JdbcTemplate jdbc;
    public ImageStorageService(@Value("${app.storage.directory:uploads}") String directory, JdbcTemplate jdbc) {
        this.directory = Path.of(directory).toAbsolutePath().normalize(); this.jdbc = jdbc;
    }

    public UploadedImage upload(MultipartFile file, UserProfile user) {
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024) throw bad("图片不能为空且不能超过10MB");
        UUID id = UUID.randomUUID();
        Path image = directory.resolve(id + ".img"), metadata = directory.resolve(id + ".properties");
        try {
            byte[] bytes = file.getBytes();
            int width, height;
            String contentType;
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                if (input == null) throw bad("请选择JPEG或PNG图片");
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw bad("请选择JPEG或PNG图片");
                var reader = readers.next();
                try {
                    String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                    if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg")) throw bad("请选择JPEG或PNG图片");
                    reader.setInput(input, true, true);
                    width = reader.getWidth(0); height = reader.getHeight(0);
                    if (width < 1 || height < 1 || (long) width * height > 20_000_000) throw bad("图片尺寸过大");
                    if (reader.read(0) == null) throw bad("图片无法读取");
                    contentType = format.equals("png") ? "image/png" : "image/jpeg";
                } catch (IOException invalidImage) {
                    throw bad("图片无法读取，请选择完整的JPEG或PNG图片");
                } finally { reader.dispose(); }
            }
            Files.createDirectories(directory);
            Files.write(image, bytes, StandardOpenOption.CREATE_NEW);
            var data = new Properties(); data.setProperty("owner", user.id().toString()); data.setProperty("contentType", contentType);
            try (var out = Files.newOutputStream(metadata, StandardOpenOption.CREATE_NEW)) { data.store(out, "Image staging metadata"); }
            return new UploadedImage(id, reference(id), width, height);
        } catch (IOException failure) {
            try { Files.deleteIfExists(image); Files.deleteIfExists(metadata); } catch (IOException ignored) { }
            throw new BusinessException("IMAGE_STORAGE_UNAVAILABLE", "图片暂时无法保存，请重试", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public StoredImage read(UUID id, UserProfile user) {
        return read(id, user, false);
    }

    /** Thumbnails are generated on first request and cached next to the original. */
    public StoredImage read(UUID id, UserProfile user, boolean thumbnail) {
        Properties metadata = metadata(id);
        boolean owner = user.id().toString().equals(metadata.getProperty("owner"));
        boolean administrator = user.role().equals("admin") || user.role().equals("superadmin");
        boolean published = published(reference(id));
        if (!owner && !administrator && !published) throw missing();
        try {
            Path file = thumbnail ? thumbnailPath(id, metadata.getProperty("contentType")) : directory.resolve(id + ".img");
            return new StoredImage(Files.readAllBytes(file), metadata.getProperty("contentType"),
                    published, Files.getLastModifiedTime(file).toMillis());
        } catch (IOException failure) { throw missing(); }
    }

    public void requireOwnedReference(String reference, UUID userId) {
        try {
            if (reference == null || !reference.startsWith("/api/v1/media/")) throw bad("请先上传图片");
            UUID id = UUID.fromString(reference.substring("/api/v1/media/".length()));
            if (!reference.equals(reference(id)) || !userId.toString().equals(metadata(id).getProperty("owner"))) throw bad("请使用本人上传的图片");
        } catch (IllegalArgumentException failure) { throw bad("图片引用不合法"); }
    }

    private Properties metadata(UUID id) {
        var result = new Properties();
        try (var in = Files.newInputStream(directory.resolve(id + ".properties"))) {
            result.load(in); return result;
        } catch (IOException failure) { throw missing(); }
    }

    private boolean published(String reference) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM foodtime.dish_images i
                    JOIN foodtime.dishes d ON d.id = i.dish_id AND d.status = 'active'
                    JOIN foodtime.stalls s ON s.id = d.stall_id AND s.status = 'active'
                    JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
                    WHERE i.image_url = ? AND i.status = 'approved'
                    UNION ALL
                    SELECT 1 FROM foodtime.stalls s
                    JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
                    WHERE s.cover_image_url = ? AND s.status = 'active'
                    UNION ALL SELECT 1 FROM foodtime.dining_halls WHERE cover_image_url = ? AND status = 'active'
                )
                """, Boolean.class, reference, reference, reference));
    }

    private String reference(UUID id) { return "/api/v1/media/" + id; }

    public record StagedFile(byte[] bytes, String contentType) { }

    /** Server-side access for moderation pipelines; never exposed without an authorization check. */
    public StagedFile stagedThumbnail(UUID id) throws IOException {
        Properties metadata = metadata(id);
        Path thumb = thumbnailPath(id, metadata.getProperty("contentType"));
        return new StagedFile(Files.readAllBytes(thumb), metadata.getProperty("contentType"));
    }

    /**
     * Returns the cached thumbnail, generating a max-480px same-format copy on first use.
     * Originals are never modified; concurrent first requests may generate twice with identical bytes.
     */
    private Path thumbnailPath(UUID id, String contentType) throws IOException {
        Path thumb = directory.resolve(id + ".thumb");
        if (Files.exists(thumb)) return thumb;
        byte[] original = Files.readAllBytes(directory.resolve(id + ".img"));
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
            if (input == null) throw new IOException("unreadable image");
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("unreadable image");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                double scale = Math.min(1.0, 480.0 / Math.max(width, height));
                int targetWidth = Math.max(1, (int) Math.round(width * scale));
                int targetHeight = Math.max(1, (int) Math.round(height * scale));
                var canvas = new java.awt.image.BufferedImage(targetWidth, targetHeight,
                        contentType.equals("image/png") ? java.awt.image.BufferedImage.TYPE_INT_ARGB
                                : java.awt.image.BufferedImage.TYPE_INT_RGB);
                var graphics = canvas.createGraphics();
                try {
                    graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    graphics.drawImage(reader.read(0), 0, 0, targetWidth, targetHeight, null);
                } finally {
                    graphics.dispose();
                }
                try (var out = Files.newOutputStream(thumb, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    String format = contentType.equals("image/png") ? "png" : "jpeg";
                    if (!ImageIO.write(canvas, format, out)) throw new IOException("thumbnail encode failed");
                }
                return thumb;
            } finally {
                reader.dispose();
            }
        }
    }
    private BusinessException missing() { return new BusinessException("IMAGE_NOT_FOUND", "图片不存在或不可访问", HttpStatus.NOT_FOUND); }
    private BusinessException bad(String message) { return new BusinessException("IMAGE_INVALID", message, HttpStatus.BAD_REQUEST); }
}
