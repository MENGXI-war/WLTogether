package com.wltogether.service;

import com.wltogether.model.entity.User;
import com.wltogether.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${storage.avatar-dir:./data/avatars}")
    private String avatarDir;

    @Value("${upload.avatar.max-size:2097152}")
    private long avatarMaxSize;

    @Value("${upload.avatar.target-width:256}")
    private int targetWidth;

    @Value("${upload.avatar.target-height:256}")
    private int targetHeight;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/png", "image/jpeg", "image/webp"
    );

    /**
     * Change user password after verifying the current password.
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("当前密码不正确");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Upload and process avatar image.
     * Resizes to target dimensions, converts to PNG, saves to disk.
     * Returns the avatar URL path.
     */
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) throws IOException {
        // Validate
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        if (file.getSize() > avatarMaxSize) {
            throw new IllegalArgumentException("头像文件不能超过 " + (avatarMaxSize / 1024 / 1024) + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("仅支持 PNG / JPEG / WebP 格式");
        }

        // Read and resize image
        BufferedImage original;
        try (InputStream in = file.getInputStream()) {
            original = ImageIO.read(in);
        }
        if (original == null) {
            throw new IllegalArgumentException("无法解析图片文件，请确认文件未损坏");
        }

        BufferedImage resized = resizeImage(original, targetWidth, targetHeight);

        // Convert to PNG bytes
        byte[] pngBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(resized, "PNG", baos);
            pngBytes = baos.toByteArray();
        }

        // Ensure directory exists
        Path dir = Paths.get(avatarDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        // Save file: {avatarDir}/{userId}.png
        Path avatarPath = dir.resolve(userId + ".png");
        Files.write(avatarPath, pngBytes);

        // Update DB
        String avatarUrl = "/api/users/" + userId + "/avatar?t=" + System.currentTimeMillis();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        log.info("Avatar uploaded for user {}: {} ({} bytes)", userId, avatarPath, pngBytes.length);
        return avatarUrl;
    }

    /**
     * Delete user avatar. Restores default (null avatarUrl).
     */
    @Transactional
    public void deleteAvatar(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // Delete file
        Path avatarPath = Paths.get(avatarDir, userId + ".png");
        try {
            Files.deleteIfExists(avatarPath);
        } catch (IOException e) {
            log.warn("Failed to delete avatar file: {}", avatarPath, e);
        }

        user.setAvatarUrl(null);
        userRepository.save(user);
        log.info("Avatar deleted for user {}", userId);
    }

    /**
     * Get avatar image bytes. Returns null if no avatar exists.
     */
    public byte[] getAvatar(Long userId) throws IOException {
        Path avatarPath = Paths.get(avatarDir, userId + ".png");
        if (!Files.exists(avatarPath)) {
            // Check DB for custom avatarUrl that may point elsewhere
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getAvatarUrl() == null) {
                throw new IOException("No avatar");
            }
            throw new IOException("Avatar file not found on disk");
        }
        return Files.readAllBytes(avatarPath);
    }

    /**
     * Resize image to target dimensions, preserving aspect ratio by
     * scaling to cover and center-cropping if needed.
     */
    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        int srcW = original.getWidth();
        int srcH = original.getHeight();

        // Calculate crop region to get square aspect ratio
        int cropSize = Math.min(srcW, srcH);
        int cropX = (srcW - cropSize) / 2;
        int cropY = (srcH - cropSize) / 2;

        BufferedImage cropped = original.getSubimage(cropX, cropY, cropSize, cropSize);

        // Scale to target size
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(cropped, 0, 0, width, height, null);
        g.dispose();

        return resized;
    }
}
