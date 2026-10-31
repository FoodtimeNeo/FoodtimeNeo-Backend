package com.FoodtimeNeo.media.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.StorageQuotaProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Reconciles existing files as well as new uploads; no in-memory quota can reset on restart. */
@Component
public class StorageBudget {
    @FunctionalInterface public interface Write<T> { T run() throws IOException; }
    private final Path directory;
    private final StorageQuotaProperties limits;
    public StorageBudget(@Value("${app.storage.directory:uploads}") String directory, StorageQuotaProperties limits) {
        this.directory = Path.of(directory).toAbsolutePath().normalize(); this.limits = limits;
    }

    public synchronized <T> T write(UUID owner, long incoming, Write<T> operation) throws IOException {
        Files.createDirectories(directory);
        // Also serializes quota decisions between processes sharing this upload directory.
        try (var channel = FileChannel.open(directory.resolve(".quota.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) throw unavailable();
            long total = 0, owned = 0;
            var owners = new HashMap<String, String>();
            try (var entries = Files.newDirectoryStream(directory, "*.properties")) {
                for (Path entry : entries) {
                    if (!Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) continue;
                    var properties = new Properties();
                    try (var in = Files.newInputStream(entry)) { properties.load(in); }
                    catch (NoSuchFileException removedByCleanup) { continue; }
                    String name = entry.getFileName().toString();
                    owners.put(name.substring(0, name.length() - ".properties".length()), properties.getProperty("owner"));
                }
            }
            try (var entries = Files.newDirectoryStream(directory)) {
                for (Path entry : entries) {
                    if (!Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) continue;
                    long size;
                    try { size = Files.size(entry); }
                    catch (NoSuchFileException removedByCleanup) { continue; }
                    total = Math.addExact(total, size);
                    String name = entry.getFileName().toString();
                    int dot = name.indexOf('.');
                    if (dot > 0 && owner.toString().equals(owners.get(name.substring(0, dot)))) owned = Math.addExact(owned, size);
                }
            }
            if (incoming > limits.userBytes() - owned) {
                throw new BusinessException("IMAGE_STORAGE_QUOTA", "图片存储额度已用完，请联系管理员", HttpStatus.PAYLOAD_TOO_LARGE);
            }
            long free = Files.getFileStore(directory).getUsableSpace();
            if (incoming > limits.totalBytes() - total || incoming > free - limits.minFreeBytes()) throw unavailable();
            return operation.run();
        }
    }

    private BusinessException unavailable() {
        return new BusinessException("IMAGE_STORAGE_UNAVAILABLE", "图片存储空间不足或暂时繁忙，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
