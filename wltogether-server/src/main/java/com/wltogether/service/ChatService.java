package com.wltogether.service;

import com.wltogether.model.dto.ChatMessageRequest;
import com.wltogether.model.dto.ChatMessageResponse;
import com.wltogether.model.entity.ChatMessage;
import com.wltogether.model.entity.User;
import com.wltogether.repository.ChatMessageRepository;
import com.wltogether.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    @Transactional
    public ChatMessageResponse saveMessage(Long senderId, ChatMessageRequest request) {
        ChatMessage msg = ChatMessage.builder()
                .groupId(request.getGroupId())
                .senderId(senderId)
                .content(request.getContent())
                .sessionId(request.getSessionId())
                .replyToId(request.getReplyToId())
                .messageType("TEXT")
                .isPinned(false)
                .build();
        msg = chatMessageRepository.save(msg);

        User sender = userRepository.findById(senderId).orElse(null);
        return toResponse(msg, sender);
    }

    public List<ChatMessageResponse> getMessages(Long groupId, Long cursor, Integer limit) {
        int size = limit != null ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        Pageable pageable = PageRequest.of(0, size);

        List<ChatMessage> messages;
        if (cursor != null) {
            Instant cursorTime = Instant.ofEpochMilli(cursor);
            messages = chatMessageRepository.findByGroupIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                    groupId, cursorTime, pageable);
        } else {
            messages = chatMessageRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);
        }

        return messages.stream().map(m -> {
            User sender = userRepository.findById(m.getSenderId()).orElse(null);
            return toResponse(m, sender);
        }).collect(Collectors.toList());
    }

    public List<ChatMessageResponse> getPinnedMessages(Long groupId) {
        return chatMessageRepository.findByGroupIdOrderByCreatedAtDesc(groupId, Pageable.unpaged())
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsPinned()))
                .map(m -> {
                    User sender = userRepository.findById(m.getSenderId()).orElse(null);
                    return toResponse(m, sender);
                })
                .collect(Collectors.toList());
    }

    private ChatMessageResponse toResponse(ChatMessage msg, User sender) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .groupId(msg.getGroupId())
                .senderId(msg.getSenderId())
                .senderNickname(sender != null ? sender.getNickname() : null)
                .senderAvatarUrl(sender != null ? sender.getAvatarUrl() : null)
                .content(msg.getContent())
                .messageType(msg.getMessageType())
                .replyToId(msg.getReplyToId())
                .sessionId(msg.getSessionId())
                .isPinned(msg.getIsPinned())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
