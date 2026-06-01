package com.wltogether.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FileTransferService {

    @Value("${storage.temp-dir:./data}/temp")
    private String tempDir;

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024 * 1024; // 20 GB default

    // Track uploaded files: fileId -> metadata
    private final Map<String, FileEntry> files = new ConcurrentHashMap<>();

    /**
     * Store an uploaded file chunk and return a file ID.
     * Returns the file ID for subsequent operations.
     */
    public String uploadFile(InputStream inputStream, String originalFilename, long fileSize) throws IOException {
        if (fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件过大，最大支持 20GB");
        }

        String fileId = UUID.randomUUID().toString().replace("-", "");
        Path dir = Paths.get(tempDir, fileId);
        Files.createDirectories(dir);

        Path filePath = dir.resolve(originalFilename);
        try (OutputStream out = Files.newOutputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }

        files.put(fileId, new FileEntry(filePath, originalFilename, fileSize, Instant.now()));
        log.info("File uploaded: {} ({} bytes) -> {}", originalFilename, fileSize, fileId);
        return fileId;
    }

    /**
     * Get file metadata by ID.
     */
    public FileEntry getFileInfo(String fileId) {
        FileEntry entry = files.get(fileId);
        if (entry == null) {
            throw new IllegalArgumentException("文件不存在或已过期");
        }
        return entry;
    }

    /**
     * Read file bytes at a given range (for HTTP Range support).
     */
    public byte[] readFileRange(String fileId, long start, long end) throws IOException {
        FileEntry entry = getFileInfo(fileId);
        if (!Files.exists(entry.path())) {
            throw new IOException("文件已被清理");
        }
        long fileSize = Files.size(entry.path());
        long actualEnd = Math.min(end, fileSize - 1);
        if (start >= fileSize) {
            return new byte[0];
        }
        int length = (int) (actualEnd - start + 1);

        try (InputStream in = Files.newInputStream(entry.path())) {
            in.skip(start);
            byte[] buffer = new byte[length];
            int totalRead = 0;
            while (totalRead < length) {
                int read = in.read(buffer, totalRead, length - totalRead);
                if (read == -1) break;
                totalRead += read;
            }
            return buffer;
        }
    }

    /**
     * Delete a temporary file.
     */
    public void deleteFile(String fileId) {
        FileEntry entry = files.remove(fileId);
        if (entry != null) {
            try {
                Files.deleteIfExists(entry.path());
                Files.deleteIfExists(entry.path().getParent());
                log.info("File deleted: {}", fileId);
            } catch (IOException e) {
                log.warn("Failed to delete temp file {}: {}", fileId, e.getMessage());
            }
        }
    }

    /**
     * Scheduled cleanup: remove files older than 24 hours.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupOldFiles() {
        Instant cutoff = Instant.now().minusSeconds(24 * 3600);
        int cleaned = 0;
        for (Map.Entry<String, FileEntry> entry : files.entrySet()) {
            if (entry.getValue().uploadedAt().isBefore(cutoff)) {
                deleteFile(entry.getKey());
                cleaned++;
            }
        }
        if (cleaned > 0) {
            log.info("Temp file cleanup: removed {} files", cleaned);
        }
    }

    // ---- inner record ----

    public record FileEntry(Path path, String originalFilename, long fileSize, Instant uploadedAt) {}
}
