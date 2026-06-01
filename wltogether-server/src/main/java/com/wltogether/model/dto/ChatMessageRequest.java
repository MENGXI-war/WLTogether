package com.wltogether.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatMessageRequest {
    @NotNull
    private Long groupId;

    @NotBlank
    private String content;

    private Long sessionId;
    private Long replyToId;
}
