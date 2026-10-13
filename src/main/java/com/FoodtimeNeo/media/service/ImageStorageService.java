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
    public record StoredImage(byte[] bytes, String contentType) { }
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
        Properties metadata = metadata(id);
        boolean owner = user.id().toString().equals(metadata.getProperty("owner"));
        boolean administrator = user.role().equals("admin") || user.role().equals("superadmin");
        if (!owner && !administrator && !published(reference(id))) throw missing();
        try {
            return new StoredImage(Files.readAllBytes(directory.resolve(id + ".img")), metadata.getProperty("contentType"));
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
    private BusinessException missing() { return new BusinessException("IMAGE_NOT_FOUND", "图片不存在或不可访问", HttpStatus.NOT_FOUND); }
    private BusinessException bad(String message) { return new BusinessException("IMAGE_INVALID", message, HttpStatus.BAD_REQUEST); }
}
