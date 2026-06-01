package com.wltogether.repository;

import com.wltogether.model.entity.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, Long> {
    List<SessionParticipant> findBySessionId(Long sessionId);
    Optional<SessionParticipant> findBySessionIdAndUserId(Long sessionId, Long userId);
    long countBySessionId(Long sessionId);
    boolean existsBySessionIdAndUserId(Long sessionId, Long userId);
}
