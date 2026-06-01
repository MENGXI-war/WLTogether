package com.wltogether.repository;

import com.wltogether.model.entity.PlaylistTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Long> {
    List<PlaylistTrack> findByPlaylistIdOrderBySortOrderAsc(Long playlistId);
    void deleteByPlaylistId(Long playlistId);
}
