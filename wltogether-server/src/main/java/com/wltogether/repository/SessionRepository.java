package com.wltogether.repository;

import com.wltogether.model.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByGroupIdOrderByCreatedAtDesc(Long groupId);
    List<Session> findByGroupIdAndStatusOrderByCreatedAtDesc(Long groupId, String status);
    List<Session> findByStatus(String status);
    @Query("SELECT s FROM Session s WHERE s.status = :status ORDER BY s.createdAt DESC")
    Page<Session> findByStatusPage(@Param("status") String status, Pageable pageable);
}
