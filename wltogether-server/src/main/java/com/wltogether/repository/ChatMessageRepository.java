package com.wltogether.repository;

import com.wltogether.model.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByGroupIdAndCreatedAtBeforeOrderByCreatedAtDesc(
        Long groupId, Instant cursor, Pageable pageable);
    List<ChatMessage> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);
    List<ChatMessage> findByMessageTypeAndExpiresAtBefore(String messageType, Instant now);
    void deleteByGroupId(Long groupId);
}
