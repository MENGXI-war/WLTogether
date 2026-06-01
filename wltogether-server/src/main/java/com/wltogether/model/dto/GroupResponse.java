package com.wltogether.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class GroupResponse {
    private Long id;
    private String name;
    private Long ownerId;
    private String description;
    private String avatarUrl;
    private String announcement;
    private String joinMode;
    private String tags;
    private Instant lastMessageAt;
    private long memberCount;
    private Instant createdAt;
}
