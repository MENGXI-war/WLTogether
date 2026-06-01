package com.wltogether.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private Long groupId;
    private Long senderId;
    private String senderNickname;
    private String senderAvatarUrl;
    private String content;
    private String messageType;
    private Long replyToId;
    private Long sessionId;
    private Boolean isPinned;
    private Instant createdAt;
}
