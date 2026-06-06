package com.wltogether.repository;

import com.wltogether.model.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByGroupIdAndCreatedAtBeforeOrderByCreatedAtDesc(
        Long groupId, Instant cursor, Pageable pageable);
    List<ChatMessage> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);
    List<ChatMessage> findByMessageTypeAndExpiresAtBefore(String messageType, Instant now);
    void deleteByGroupId(Long groupId);
    Page<ChatMessage> findBySenderIdOrderByCreatedAtDesc(Long senderId, Pageable pageable);
    Page<ChatMessage> findByGroupIdAndSenderIdOrderByCreatedAtDesc(Long groupId, Long senderId, Pageable pageable);
    @Query("SELECT m FROM ChatMessage m WHERE m.groupId = :groupId ORDER BY m.createdAt DESC")
    Page<ChatMessage> findByGroupIdPage(@Param("groupId") Long groupId, Pageable pageable);
}
