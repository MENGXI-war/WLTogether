package com.wltogether.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class MemberResponse {
    private Long userId;
    private String uid;
    private String username;
    private String nickname;
    private String nicknameInGroup;
    private String avatarUrl;
    private String role;
    private Instant joinedAt;
}
