package com.wltogether.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "playlist_tracks")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PlaylistTrack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(length = 200)
    private String artist;

    @Column(length = 200)
    private String album;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
