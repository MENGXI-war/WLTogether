package com.wltogether.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class PlaylistResponse {
    private Long id;
    private Long groupId;
    private String name;
    private Long creatorId;
    private String creatorNickname;
    private int trackCount;
    private List<TrackResponse> tracks;
    private Instant createdAt;
}
