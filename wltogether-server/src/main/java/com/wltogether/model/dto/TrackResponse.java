package com.wltogether.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TrackResponse {
    private Long id;
    private Long playlistId;
    private String fileHash;
    private String fileName;
    private Integer durationSeconds;
    private String artist;
    private String album;
    private Integer sortOrder;
}
