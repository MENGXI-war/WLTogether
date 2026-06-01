package com.wltogether.controller;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.dto.ChatMessageResponse;
import com.wltogether.model.entity.ChatMessage;
import com.wltogether.model.entity.User;
import com.wltogether.repository.UserRepository;
import com.wltogether.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    /**
     * Upload image for chat. Returns a ChatMessage that the client should
     * broadcast via WebSocket.
     */
    @PostMapping("/groups/{groupId}/images")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> uploadImage(
            Authentication auth,
            @PathVariable Long groupId,
            @RequestParam("file") MultipartFile file) {
        Long userId = (Long) auth.getPrincipal();
        try {
            ChatMessage message = fileStorageService.uploadChatImage(groupId, userId, file);
            return ResponseEntity.ok(ApiResponse.ok("ok", toResponse(message)));
        } catch (IOException e) {
            throw new IllegalArgumentException("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * Get uploaded image binary.
     */
    @GetMapping("/files/images/{groupId}/{messageId}")
    public ResponseEntity<byte[]> getImage(
            @PathVariable Long groupId,
            @PathVariable Long messageId,
            @RequestParam(value = "thumb", defaultValue = "0") int thumb) {
        try {
            byte[] bytes = fileStorageService.getChatImage(groupId, messageId, thumb == 1);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get group storage usage stats.
     */
    @GetMapping("/groups/{groupId}/storage")
    public ResponseEntity<ApiResponse<FileStorageService.StorageStats>> getStorageStats(
            Authentication auth,
            @PathVariable Long groupId) {
        return ResponseEntity.ok(ApiResponse.ok("ok", fileStorageService.getStorageStats(groupId)));
    }

    private ChatMessageResponse toResponse(ChatMessage msg) {
        User sender = userRepository.findById(msg.getSenderId()).orElse(null);
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .groupId(msg.getGroupId())
                .senderId(msg.getSenderId())
                .senderNickname(sender != null ? sender.getNickname() : null)
                .senderAvatarUrl(sender != null ? sender.getAvatarUrl() : null)
                .content(msg.getContent())
                .messageType(msg.getMessageType())
                .imageThumbnailUrl(msg.getImageThumbnailUrl())
                .replyToId(msg.getReplyToId())
                .sessionId(msg.getSessionId())
                .isPinned(msg.getIsPinned())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
