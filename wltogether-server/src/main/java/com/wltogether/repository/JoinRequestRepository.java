package com.wltogether.repository;

import com.wltogether.model.entity.JoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {
    List<JoinRequest> findByGroupIdAndStatusOrderByCreatedAtAsc(Long groupId, String status);
    Optional<JoinRequest> findByGroupIdAndUserIdAndStatus(Long groupId, Long userId, String status);
    boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, String status);
}
