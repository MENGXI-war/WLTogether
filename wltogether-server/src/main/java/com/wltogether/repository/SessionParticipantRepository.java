package com.wltogether.repository;

import com.wltogether.model.entity.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, Long> {
    List<SessionParticipant> findBySessionId(Long sessionId);
    List<SessionParticipant> findByUserId(Long userId);
    Optional<SessionParticipant> findBySessionIdAndUserId(Long sessionId, Long userId);
    long countBySessionId(Long sessionId);
    boolean existsBySessionIdAndUserId(Long sessionId, Long userId);
    void deleteBySessionIdAndUserId(Long sessionId, Long userId);
}
