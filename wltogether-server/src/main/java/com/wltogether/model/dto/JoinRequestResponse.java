package com.wltogether.model.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class JoinRequestResponse {
    private Long id;
    private Long groupId;
    private Long userId;
    private String username;
    private String nickname;
    private String status;
    private Instant createdAt;
}
