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
    public enum CachePolicy { PUBLIC_IMMUTABLE, PRIVATE_REVALIDATE, NO_STORE }
    public record ImageInfo(UUID id, String contentType, CachePolicy policy, long lastModified) { }
    private final Path directory;
    private final JdbcTemplate jdbc;
    private final StorageBudget budget;
    private final java.util.concurrent.Semaphore uploads;
    public ImageStorageService(@Value("${app.storage.directory:uploads}") String directory, JdbcTemplate jdbc, StorageBudget budget,
                               com.FoodtimeNeo.config.properties.StorageQuotaProperties limits) {
        this.directory = Path.of(directory).toAbsolutePath().normalize(); this.jdbc = jdbc;
        this.budget = budget; this.uploads = new java.util.concurrent.Semaphore(limits.concurrentUploads());
    }

    public UploadedImage upload(MultipartFile file, UserProfile user) {
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024) throw bad("图片不能为空且不能超过10MB");
        if (!uploads.tryAcquire()) throw new UploadBusyException();
        UUID id = UUID.randomUUID();
        Path image = directory.resolve(id + ".img"), metadata = directory.resolve(id + ".properties");
        try {
            var normalized = ImageSanitizer.sanitize(file.getBytes());
            return budget.write(user.id(), normalized.bytes().length + 1024L, () -> {
                try {
                    Files.write(image, normalized.bytes(), StandardOpenOption.CREATE_NEW);
                    var data = new Properties();
                    data.setProperty("owner", user.id().toString());
                    data.setProperty("contentType", normalized.contentType());
                    data.setProperty("width", String.valueOf(normalized.width()));
                    data.setProperty("height", String.valueOf(normalized.height()));
                    try (var out = Files.newOutputStream(metadata, StandardOpenOption.CREATE_NEW)) { data.store(out, "Image staging metadata"); }
                    return new UploadedImage(id, reference(id), normalized.width(), normalized.height());
                } catch (IOException failure) {
                    Files.deleteIfExists(image); Files.deleteIfExists(metadata); throw failure;
                }
            });
        } catch (IOException failure) {
            throw new BusinessException("IMAGE_STORAGE_UNAVAILABLE", "图片暂时无法保存，请重试", HttpStatus.SERVICE_UNAVAILABLE);
        } finally { uploads.release(); }
    }

    private static class UploadBusyException extends com.FoodtimeNeo.common.exception.RateLimitedException {
        UploadBusyException() { super("IMAGE_PROCESSING_BUSY", "图片处理繁忙，请稍后重试", 1); }
    }

    public StoredImage read(UUID id, UserProfile user) {
        return read(id, user, false);
    }

    /** Thumbnails are generated on first request and cached next to the original. */
    public StoredImage read(UUID id, UserProfile user, boolean thumbnail) {
        ImageInfo info = inspect(id, user);
        return new StoredImage(readBytes(info, thumbnail), info.contentType(),
                info.policy() == CachePolicy.PUBLIC_IMMUTABLE, info.lastModified());
    }

    /** Authorize before evaluating validators, without reading/decoding the image body. */
    public ImageInfo inspect(UUID id, UserProfile user) {
        Properties metadata = metadata(id);
        if (user == null) throw missing();
        boolean owner = user.id().toString().equals(metadata.getProperty("owner"));
        boolean administrator = user.role().equals("admin") || user.role().equals("superadmin");
        Publication publication = publication(reference(id));
        boolean published = publication.catalog() || publication.community() || publication.avatar();
        if (!owner && !administrator && !published) throw missing();
        try {
            Path file = directory.resolve(id + ".img");
            if (!Files.isRegularFile(file)) throw missing();
            CachePolicy policy = publication.catalog() ? CachePolicy.PUBLIC_IMMUTABLE
                    : published ? CachePolicy.PRIVATE_REVALIDATE : CachePolicy.NO_STORE;
            return new ImageInfo(id, metadata.getProperty("contentType"), policy,
                    Files.getLastModifiedTime(file).toMillis());
        } catch (IOException failure) { throw missing(); }
    }

    public byte[] readBytes(ImageInfo info, boolean thumbnail) {
        try {
            return Files.readAllBytes(thumbnail ? thumbnailPath(info.id(), info.contentType())
                    : directory.resolve(info.id() + ".img"));
        } catch (IOException failure) { throw missing(); }
    }

    public void requireOwnedReference(String reference, UUID userId) {
        try {
            if (reference == null || !reference.startsWith("/api/v1/media/")) throw bad("请先上传图片");
            UUID id = UUID.fromString(reference.substring("/api/v1/media/".length()));
            if (!reference.equals(reference(id)) || !userId.toString().equals(metadata(id).getProperty("owner"))) throw bad("请使用本人上传的图片");
        } catch (IllegalArgumentException failure) { throw bad("图片引用不合法"); }
    }

    public UploadedImage ownedImage(String reference,UUID userId) {
        requireOwnedReference(reference,userId);
        UUID id=UUID.fromString(reference.substring("/api/v1/media/".length()));
        var data=metadata(id);
        try {
            int width,height;
            if(data.containsKey("width")){width=Integer.parseInt(data.getProperty("width"));height=Integer.parseInt(data.getProperty("height"));}
            else try(var input=ImageIO.createImageInputStream(directory.resolve(id+".img").toFile())) {
                var readers=ImageIO.getImageReaders(input);if(!readers.hasNext()) throw missing();
                var reader=readers.next();try{reader.setInput(input);width=reader.getWidth(0);height=reader.getHeight(0);}finally{reader.dispose();}
            }
            if(width<1||height<1||(long)width*height>20_000_000)throw bad("图片尺寸过大");
            return new UploadedImage(id,reference,width,height);
        }catch(IOException|NumberFormatException e){throw missing();}
    }

    private Properties metadata(UUID id) {
        var result = new Properties();
        try (var in = Files.newInputStream(directory.resolve(id + ".properties"))) {
            result.load(in); return result;
        } catch (IOException failure) { throw missing(); }
    }

    private record Publication(boolean catalog,boolean community,boolean avatar) { }

    private Publication publication(String reference) {
        return jdbc.queryForObject("""
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
                    UNION ALL
                    SELECT 1 FROM foodtime.stall_images i
                    JOIN foodtime.stalls s ON s.id = i.stall_id AND s.status = 'active'
                    JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
                    WHERE i.image_url = ?
                    UNION ALL SELECT 1 FROM foodtime.dining_halls WHERE cover_image_url = ? AND status = 'active'
                ) AS catalog,
                EXISTS (SELECT 1 FROM foodtime.community_post_images i JOIN foodtime.community_posts p ON p.id=i.post_id
                        WHERE i.image_url=? AND p.status='approved' AND p.deleted_at IS NULL
                ) AS community,
                EXISTS (SELECT 1 FROM foodtime.users WHERE avatar_url=? AND status='active') AS avatar
                """, (r,n)->new Publication(r.getBoolean("catalog"),r.getBoolean("community"),r.getBoolean("avatar")), reference, reference, reference, reference, reference, reference);
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
        if (!uploads.tryAcquire()) throw new UploadBusyException();
        try {
            // A concurrent request may have completed the same thumbnail while we waited.
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
                    var encoded = new ByteArrayOutputStream();
                    String format = contentType.equals("image/png") ? "png" : "jpeg";
                    if (!ImageIO.write(canvas, format, encoded)) throw new IOException("thumbnail encode failed");
                    byte[] thumbnail = encoded.toByteArray();
                    budget.write(UUID.fromString(metadata(id).getProperty("owner")), thumbnail.length, () -> {
                        if (Files.exists(thumb)) return thumb;
                        Path temporary = Files.createTempFile(directory, id + "-", ".thumb-tmp");
                        try {
                            Files.write(temporary, thumbnail);
                            try { Files.move(temporary, thumb, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                            catch (AtomicMoveNotSupportedException unsupported) {
                                Files.move(temporary, thumb, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } finally { Files.deleteIfExists(temporary); }
                        return thumb;
                    });
                    return thumb;
                } finally {
                    reader.dispose();
                }
            }
        } finally { uploads.release(); }
    }

    private BusinessException missing() { return new BusinessException("IMAGE_NOT_FOUND", "图片不存在或不可访问", HttpStatus.NOT_FOUND); }
    private BusinessException bad(String message) { return new BusinessException("IMAGE_INVALID", message, HttpStatus.BAD_REQUEST); }
}
