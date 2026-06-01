package com.wltogether.repository;

import com.wltogether.model.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByGroupIdOrderByCreatedAtDesc(Long groupId);
}
