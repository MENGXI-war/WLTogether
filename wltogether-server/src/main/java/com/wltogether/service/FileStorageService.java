package com.wltogether.service;

import com.wltogether.model.entity.ChatMessage;
import com.wltogether.model.entity.Group;
import com.wltogether.repository.ChatMessageRepository;
import com.wltogether.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final GroupRepository groupRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Value("${storage.image-dir:./data}/images")
    private String imageDir;

    @Value("${storage.image-max-size:5242880}")
    private long imageMaxSize;

    @Value("${storage.max-per-group-mb:1024}")
    private int maxPerGroupMb;

    @Value("${storage.message-retention-days:7}")
    private int retentionDays;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/png", "image/jpeg", "image/webp", "image/gif"
    );

    /**
     * Upload a chat image. Stores compressed version + thumbnail.
     * Returns a ChatMessage with type=IMAGE and content=image URL.
     */
    @Transactional
    public ChatMessage uploadChatImage(Long groupId, Long senderId, MultipartFile file) throws IOException {
        // Validate
        if (file.isEmpty()) throw new IllegalArgumentException("文件为空");
        if (file.getSize() > imageMaxSize) {
            throw new IllegalArgumentException("图片不能超过 " + (imageMaxSize / 1024 / 1024) + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("仅支持 PNG / JPEG / WebP / GIF 格式");
        }

        // Check group storage quota
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("群组不存在"));
        long maxBytes = (long) maxPerGroupMb * 1024 * 1024;
        if (group.getStorageUsedBytes() + file.getSize() > maxBytes) {
            throw new IllegalArgumentException("群组存储空间不足（上限 " + maxPerGroupMb + "MB），请清理旧文件后重试");
        }

        // Read and process image
        BufferedImage original;
        try (InputStream in = file.getInputStream()) {
            original = ImageIO.read(in);
        }
        if (original == null) {
            throw new IllegalArgumentException("无法解析图片文件，请确认文件未损坏");
        }

        // Generate paths
        Instant expiresAt = Instant.now().plusSeconds((long) retentionDays * 24 * 3600);

        // Create chat message first to get ID
        ChatMessage message = ChatMessage.builder()
                .groupId(groupId)
                .senderId(senderId)
                .messageType("IMAGE")
                .content(null) // will be updated
                .expiresAt(expiresAt)
                .build();
        message = chatMessageRepository.save(message);

        // Ensure directory
        Path groupDir = Paths.get(imageDir, String.valueOf(groupId));
        if (!Files.exists(groupDir)) {
            Files.createDirectories(groupDir);
        }

        // Save compressed full image (max 1920px wide)
        BufferedImage compressed = compressImage(original, 1920);
        byte[] imageBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(compressed, "webp" != null ? "PNG" : "PNG", baos);
            imageBytes = baos.toByteArray();
        }
        Path imagePath = groupDir.resolve(message.getId() + ".png");
        Files.write(imagePath, imageBytes);

        // Save thumbnail (200x200)
        BufferedImage thumbnail = resizeImage(original, 200, 200);
        byte[] thumbBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(thumbnail, "PNG", baos);
            thumbBytes = baos.toByteArray();
        }
        Path thumbPath = groupDir.resolve(message.getId() + "_thumb.png");
        Files.write(thumbPath, thumbBytes);

        // Calculate total stored bytes
        long totalBytes = imageBytes.length + thumbBytes.length;

        // Update message with URLs
        String imageUrl = "/api/files/images/" + groupId + "/" + message.getId();
        String thumbUrl = imageUrl + "?thumb=1";
        message.setContent(imageUrl);
        message.setImageThumbnailUrl(thumbUrl);
        chatMessageRepository.save(message);

        // Update group storage usage
        group.setStorageUsedBytes(group.getStorageUsedBytes() + totalBytes);
        groupRepository.save(group);

        log.info("Chat image uploaded: group={}, message={}, size={} bytes, expires={}",
                groupId, message.getId(), totalBytes, expiresAt);
        return message;
    }

    /**
     * Get image bytes for a chat message.
     */
    public byte[] getChatImage(Long groupId, Long messageId, boolean thumbnail) throws IOException {
        String filename = messageId + (thumbnail ? "_thumb.png" : ".png");
        Path filePath = Paths.get(imageDir, String.valueOf(groupId), filename);
        if (!Files.exists(filePath)) {
            throw new IOException("Image file not found");
        }
        return Files.readAllBytes(filePath);
    }

    /**
     * Get group storage stats.
     */
    public StorageStats getStorageStats(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("群组不存在"));
        long maxBytes = (long) maxPerGroupMb * 1024 * 1024;
        return new StorageStats(
                group.getStorageUsedBytes(),
                maxBytes,
                maxPerGroupMb
        );
    }

    /**
     * Scheduled cleanup: runs every hour, deletes expired images and updates storage.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredImages() {
        Instant now = Instant.now();
        List<ChatMessage> expired = chatMessageRepository
                .findByMessageTypeAndExpiresAtBefore("IMAGE", now);

        int cleaned = 0;
        long freedBytes = 0;

        for (ChatMessage msg : expired) {
            try {
                Long groupId = msg.getGroupId();
                Path groupDir = Paths.get(imageDir, String.valueOf(groupId));

                // Delete full image
                Path imagePath = groupDir.resolve(msg.getId() + ".png");
                long imageSize = Files.exists(imagePath) ? Files.size(imagePath) : 0;
                Files.deleteIfExists(imagePath);

                // Delete thumbnail
                Path thumbPath = groupDir.resolve(msg.getId() + "_thumb.png");
                long thumbSize = Files.exists(thumbPath) ? Files.size(thumbPath) : 0;
                Files.deleteIfExists(thumbPath);

                freedBytes += imageSize + thumbSize;

                // Update group storage
                Group group = groupRepository.findById(groupId).orElse(null);
                if (group != null) {
                    group.setStorageUsedBytes(Math.max(0, group.getStorageUsedBytes() - imageSize - thumbSize));
                    groupRepository.save(group);
                }

                // Mark message
                msg.setContent("[已过期]");
                msg.setImageThumbnailUrl(null);
                msg.setExpiresAt(null);
                chatMessageRepository.save(msg);

                cleaned++;
            } catch (IOException e) {
                log.warn("Failed to clean up image for message {}: {}", msg.getId(), e.getMessage());
            }
        }

        if (cleaned > 0) {
            log.info("Cleanup complete: removed {} expired images, freed {} bytes", cleaned, freedBytes);
        }
    }

    private BufferedImage compressImage(BufferedImage original, int maxWidth) {
        int w = original.getWidth();
        int h = original.getHeight();
        if (w <= maxWidth) return original;

        int newH = (int) ((double) maxWidth / w * h);
        BufferedImage resized = new BufferedImage(maxWidth, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, maxWidth, newH, null);
        g.dispose();
        return resized;
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        int srcW = original.getWidth();
        int srcH = original.getHeight();
        int cropSize = Math.min(srcW, srcH);
        int cropX = (srcW - cropSize) / 2;
        int cropY = (srcH - cropSize) / 2;

        BufferedImage cropped = original.getSubimage(cropX, cropY, cropSize, cropSize);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(cropped, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    // ---- inner class ----

    public record StorageStats(long usedBytes, long maxBytes, int maxMb) {
        public double usedPercent() {
            return maxBytes > 0 ? (double) usedBytes / maxBytes * 100 : 0;
        }
    }
}
