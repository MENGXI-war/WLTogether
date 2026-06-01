package com.wltogether.service;

import com.wltogether.model.dto.CreateSessionRequest;
import com.wltogether.model.dto.SessionResponse;
import com.wltogether.model.entity.Session;
import com.wltogether.model.entity.SessionParticipant;
import com.wltogether.repository.GroupMemberRepository;
import com.wltogether.repository.SessionParticipantRepository;
import com.wltogether.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionParticipantRepository participantRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Value("${session.max-participants:20}")
    private int maxParticipants;

    @Value("${session.max-concurrent-per-user:5}")
    private int maxConcurrentPerUser;

    @Transactional
    public SessionResponse createSession(Long groupId, Long userId, CreateSessionRequest request) {
        ensureMember(groupId, userId);

        // Check concurrent session limit
        long concurrent = sessionRepository.findByStatus("ACTIVE").stream()
                .filter(s -> s.getHostId().equals(userId)).count();
        if (concurrent >= maxConcurrentPerUser) {
            throw new IllegalArgumentException("您发起的活跃会话已达上限 (" + maxConcurrentPerUser + ")");
        }

        Session session = Session.builder()
                .groupId(groupId)
                .hostId(userId)
                .fileHash(request.getFileHash())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .durationSeconds(request.getDuration())
                .mediaType(request.getMediaType())
                .videoCodec(request.getVideoCodec())
                .audioCodec(request.getAudioCodec())
                .magnetUri(request.getMagnetUri())
                .status("CREATED")
                .build();
        session = sessionRepository.save(session);

        // Host joins automatically
        joinSessionInternal(session.getId(), userId);

        return toResponse(session);
    }

    public List<SessionResponse> listSessions(Long groupId, Long userId) {
        ensureMember(groupId, userId);
        return sessionRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public SessionResponse getSession(Long sessionId, Long userId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
        ensureMember(session.getGroupId(), userId);
        return toResponse(session);
    }

    @Transactional
    public SessionResponse joinSession(Long sessionId, Long userId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));

        if ("ENDED".equals(session.getStatus())) {
            throw new IllegalArgumentException("会话已结束");
        }

        ensureMember(session.getGroupId(), userId);

        long count = participantRepository.countBySessionId(sessionId);
        if (count >= maxParticipants) {
            throw new IllegalArgumentException("会话参与人数已满 (" + maxParticipants + ")");
        }

        if (!participantRepository.existsBySessionIdAndUserId(sessionId, userId)) {
            joinSessionInternal(sessionId, userId);
        }

        // Activate session on first join
        if ("CREATED".equals(session.getStatus())) {
            session.setStatus("ACTIVE");
            session.setStartedAt(Instant.now());
            sessionRepository.save(session);
        }

        return toResponse(session);
    }

    @Transactional
    public void endSession(Long sessionId, Long userId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));

        if (!session.getHostId().equals(userId)) {
            throw new IllegalArgumentException("仅发起者可结束会话");
        }

        session.setStatus("ENDED");
        session.setEndedAt(Instant.now());
        sessionRepository.save(session);
    }

    private void joinSessionInternal(Long sessionId, Long userId) {
        SessionParticipant participant = SessionParticipant.builder()
                .sessionId(sessionId)
                .userId(userId)
                .fileStatus("LOCAL")
                .build();
        participantRepository.save(participant);
    }

    private void ensureMember(Long groupId, Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new IllegalArgumentException("你不在此群组中");
        }
    }

    private SessionResponse toResponse(Session session) {
        long count = participantRepository.countBySessionId(session.getId());
        return SessionResponse.builder()
                .id(session.getId())
                .groupId(session.getGroupId())
                .hostId(session.getHostId())
                .fileHash(session.getFileHash())
                .fileName(session.getFileName())
                .fileSize(session.getFileSize())
                .durationSeconds(session.getDurationSeconds())
                .mediaType(session.getMediaType())
                .videoCodec(session.getVideoCodec())
                .audioCodec(session.getAudioCodec())
                .magnetUri(session.getMagnetUri())
                .status(session.getStatus())
                .participantCount(count)
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
