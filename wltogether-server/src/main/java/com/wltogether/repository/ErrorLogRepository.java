package com.wltogether.repository;

import com.wltogether.model.entity.ErrorLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
    List<ErrorLog> findByStatusOrderByOccurredAtDesc(String status, Pageable pageable);
    List<ErrorLog> findBySeverityOrderByOccurredAtDesc(String severity, Pageable pageable);
}
