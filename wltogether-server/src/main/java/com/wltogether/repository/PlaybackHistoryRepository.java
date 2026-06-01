package com.wltogether.repository;

import com.wltogether.model.entity.PlaybackHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlaybackHistoryRepository extends JpaRepository<PlaybackHistory, Long> {
    Optional<PlaybackHistory> findBySessionId(Long sessionId);
}
