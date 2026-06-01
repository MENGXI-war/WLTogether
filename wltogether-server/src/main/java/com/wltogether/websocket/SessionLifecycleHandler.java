package com.wltogether.websocket;

import com.wltogether.model.dto.CreateSessionRequest;
import com.wltogether.model.dto.SessionResponse;
import com.wltogether.model.entity.Session;
import com.wltogether.model.entity.User;
import com.wltogether.repository.UserRepository;
import com.wltogether.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SessionLifecycleHandler {

    private final SessionService sessionService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/session.start")
    public void handleSessionStart(Map<String, Object> payload, Principal principal) {
        Long userId = extractUserId(principal);
        Long groupId = toLong(payload.get("groupId"));

        if (groupId == null) {
            sendError(principal, "INVALID_MESSAGE", "缺少 groupId");
            return;
        }

        try {
            CreateSessionRequest request = new CreateSessionRequest();
            request.setFileHash((String) payload.getOrDefault("fileHash", ""));
            request.setFileName((String) payload.getOrDefault("fileName", ""));
            request.setFileSize(toLong(payload.get("fileSize")));
            request.setMediaType((String) payload.getOrDefault("mediaType", "VIDEO"));
            request.setVideoCodec((String) payload.get("videoCodec"));
            request.setAudioCodec((String) payload.get("audioCodec"));
            request.setMagnetUri((String) payload.get("magnetUri"));
            Object duration = payload.get("duration");
            if (duration instanceof Number n) request.setDuration(n.intValue());

            SessionResponse session = sessionService.createSession(groupId, userId, request);

            // Broadcast to group
            Map<String, Object> broadcast = Map.of(
                    "type", "session.created",
                    "sessionId", session.getId(),
                    "hostId", session.getHostId(),
                    "fileHash", session.getFileHash(),
                    "fileName", session.getFileName() != null ? session.getFileName() : "",
                    "mediaType", session.getMediaType(),
                    "magnetUri", session.getMagnetUri() != null ? session.getMagnetUri() : "",
                    "transferMode", payload.getOrDefault("transferMode", "p2pDirect")
            );
            messagingTemplate.convertAndSend("/topic/group/" + groupId, broadcast);
        } catch (Exception e) {
            sendError(principal, "SERVER_ERROR", e.getMessage());
        }
    }

    @MessageMapping("/session.join")
    public void handleSessionJoin(Map<String, Object> payload, Principal principal) {
        Long userId = extractUserId(principal);
        Long sessionId = toLong(payload.get("sessionId"));

        if (sessionId == null) {
            sendError(principal, "INVALID_MESSAGE", "缺少 sessionId");
            return;
        }

        try {
            SessionResponse session = sessionService.joinSession(sessionId, userId);
            User user = userRepository.findById(userId).orElse(null);

            Map<String, Object> broadcast = Map.of(
                    "type", "session.user_joined",
                    "sessionId", sessionId,
                    "userId", userId,
                    "nickname", user != null ? user.getNickname() : ""
            );
            messagingTemplate.convertAndSend("/topic/group/" + session.getGroupId(), broadcast);
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, broadcast);
        } catch (Exception e) {
            sendError(principal, "INVALID_MESSAGE", e.getMessage());
        }
    }

    @MessageMapping("/session.leave")
    public void handleSessionLeave(Map<String, Object> payload, Principal principal) {
        Long userId = extractUserId(principal);
        Long sessionId = toLong(payload.get("sessionId"));

        if (sessionId == null) {
            sendError(principal, "INVALID_MESSAGE", "缺少 sessionId");
            return;
        }

        User user = userRepository.findById(userId).orElse(null);

        Map<String, Object> broadcast = Map.of(
                "type", "session.user_left",
                "sessionId", sessionId,
                "userId", userId,
                "nickname", user != null ? user.getNickname() : ""
        );
        messagingTemplate.convertAndSend("/topic/session/" + sessionId, broadcast);
    }

    @MessageMapping("/session.end")
    public void handleSessionEnd(Map<String, Object> payload, Principal principal) {
        Long userId = extractUserId(principal);
        Long sessionId = toLong(payload.get("sessionId"));

        if (sessionId == null) {
            sendError(principal, "INVALID_MESSAGE", "缺少 sessionId");
            return;
        }

        try {
            sessionService.endSession(sessionId, userId);

            Map<String, Object> broadcast = Map.of(
                    "type", "session.ended",
                    "sessionId", sessionId
            );
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, broadcast);
        } catch (Exception e) {
            sendError(principal, "PERMISSION_DENIED", e.getMessage());
        }
    }

    private Long extractUserId(Principal principal) {
        if (principal instanceof WsAuthInterceptor.StompPrincipal p) {
            return p.userId();
        }
        return Long.parseLong(principal.getName());
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private void sendError(Principal principal, String code, String message) {
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/error",
                Map.of("type", "error", "code", code, "message", message));
    }
}
