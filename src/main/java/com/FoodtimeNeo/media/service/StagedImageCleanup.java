package com.FoodtimeNeo.media.service;

import com.FoodtimeNeo.config.properties.StorageCleanupProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.*;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Deletes staged upload files that were never referenced by any submission, cover or avatar.
 * Referenced files are always kept; only the local staging bytes and their metadata are removed.
 */
@Component
@ConditionalOnProperty(prefix = "app.storage", name = "cleanup-enabled", havingValue = "true", matchIfMissing = true)
public class StagedImageCleanup {
    private static final Logger LOG = LoggerFactory.getLogger(StagedImageCleanup.class);
    private final Path directory;
    private final JdbcTemplate jdbc;
    private final StorageCleanupProperties properties;
    private final Clock clock;

    public StagedImageCleanup(@Value("${app.storage.directory:uploads}") String directory, JdbcTemplate jdbc,
                              StorageCleanupProperties properties, Clock clock) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.jdbc = jdbc; this.properties = properties; this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.storage.cleanup-interval:PT24H}",
            initialDelayString = "${app.storage.cleanup-initial-delay:PT5M}")
    public void clean() {
        try {
            Instant cutoff = clock.instant().minus(properties.cleanupMaxAge());
            List<String> stale = stagedBefore(cutoff);
            int removed = 0;
            for (String id : stale) {
                if (!referenced("/api/v1/media/" + id) && delete(id)) removed++;
            }
            long bytes = directoryBytes();
            if (removed > 0 || !stale.isEmpty()) {
                LOG.info("Cleaned {} orphan staged images ({} stale), staging directory holds {} bytes",
                        removed, stale.size(), bytes);
            }
            if (bytes > properties.diskWarnBytes()) {
                LOG.warn("Staging directory holds {} bytes, above the {} byte warning line", bytes,
                        properties.diskWarnBytes());
            }
        } catch (RuntimeException exception) {
            LOG.error("Failed to clean orphan staged images", exception);
        }
    }

    private long directoryBytes() {
        try (var files = Files.list(directory)) {
            return files.mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException ignored) {
                    return 0L;
                }
            }).sum();
        } catch (IOException missing) {
            return 0L;
        }
    }

    private List<String> stagedBefore(Instant cutoff) {
        var result = new ArrayList<String>();
        try (var files = Files.list(directory)) {
            files.filter(path -> path.getFileName().toString().endsWith(".properties")).forEach(metadata -> {
                try {
                    String name = metadata.getFileName().toString();
                    UUID.fromString(name.substring(0, name.length() - ".properties".length()));
                    if (Files.getLastModifiedTime(metadata).toInstant().isBefore(cutoff)
                            && Files.getLastModifiedTime(directory.resolve(name.replace(".properties", ".img")))
                                    .toInstant().isBefore(cutoff)) {
                        result.add(name.substring(0, name.length() - ".properties".length()));
                    }
                } catch (IllegalArgumentException | IOException ignored) {
                    // Foreign or unreadable files are left alone.
                }
            });
        } catch (IOException missing) {
            // No staging directory yet; nothing to clean.
        }
        return result;
    }

    private boolean referenced(String reference) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM foodtime.dish_images WHERE image_url = ?
                    UNION ALL SELECT 1 FROM foodtime.stalls WHERE cover_image_url = ?
                    UNION ALL SELECT 1 FROM foodtime.stall_images WHERE image_url = ?
                    UNION ALL SELECT 1 FROM foodtime.dining_halls WHERE cover_image_url = ?
                    UNION ALL SELECT 1 FROM foodtime.users WHERE avatar_url = ?
                    UNION ALL SELECT 1 FROM foodtime.community_post_images WHERE image_url = ?
                )
                """, Boolean.class, reference, reference, reference, reference, reference, reference));
    }

    private boolean delete(String id) {
        try {
            Files.deleteIfExists(directory.resolve(id + ".img"));
            Files.deleteIfExists(directory.resolve(id + ".thumb"));
            Files.deleteIfExists(directory.resolve(id + ".properties"));
            return true;
        } catch (IOException failure) {
            LOG.error("Failed to delete orphan staged image {}", id);
            return false;
        }
    }
}
