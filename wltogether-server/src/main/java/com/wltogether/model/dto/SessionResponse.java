package com.wltogether.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class SessionResponse {
    private Long id;
    private Long groupId;
    private Long hostId;
    private String fileHash;
    private String fileName;
    private Long fileSize;
    private Integer durationSeconds;
    private String mediaType;
    private String videoCodec;
    private String audioCodec;
    private String magnetUri;
    private String status;
    private long participantCount;
    private Instant startedAt;
    private Instant endedAt;
    private Instant createdAt;
}
