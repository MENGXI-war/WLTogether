package com.wltogether.repository;

import com.wltogether.model.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByGroupIdOrderByCreatedAtDesc(Long groupId);
    List<Session> findByGroupIdAndStatusOrderByCreatedAtDesc(Long groupId, String status);
    List<Session> findByStatus(String status);
}
