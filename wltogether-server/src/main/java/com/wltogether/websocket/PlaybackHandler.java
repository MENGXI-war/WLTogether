package com.wltogether.websocket;

import com.wltogether.service.PlaybackSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PlaybackHandler {

    private final PlaybackSyncService playbackSyncService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/playback.play")
    public void handlePlay(Map<String, Object> payload, Principal principal) {
        Long userId = extractUserId(principal);
        Long sessionId = toLong(payload.get("sessionId"));

        if (sessionId == null) {
            sendError(principal, "INVALID_MESSAGE", "缺少 sessionId");
            return;
        }

        if (!playbackSyncService.canControl(sessionId, userId)) {
            sendError(principal, "PERMISSION_DENIED", "当前播放模式下无权执行此操作");
            return;
        }

        broadcastPlaybackState(sessionId, payload, "play", principal);
    }

    @MessageMapping("/playback.pause")
    public void handlePause(Map<String, Object> payload, Principal principal) {
        Long userId = extractUserId(principal);
        Long sessionId = toLong(payload.get("sessionId"));

        if (sessionId == null) {
            sendError(principal, "INVALID_MESSAGE", "缺少 sessionId");
            return;
        }

        if (!playbackSyncService.canControl(sessionId, userId)) {
            sendError(principal, "PERMISSION_DENIED", "当前播放模式下无权执行此操作");
            return;
        }

        broadcastPlaybackState(sessionId, payload, "pause", principal);
    }

    @MessageMapping("/playback.seek")
    public void handleSeek(Map<String, Object> payload, Principal principal) {
        Long userId = extractUserId(principal);
        Long sessionId = toLong(payload.get("sessionId"));

        if (sessionId == null) {
            sendError(principal, "INVALID_MESSAGE", "缺少 sessionId");
            return;
        }

        if (playbackSyncService.isSeekDebounced(sessionId, userId)) {
            sendError(principal, "RATE_LIMITED", "seek 操作过于频繁，请 3 秒后重试");
            return;
        }

        if (!playbackSyncService.canControl(sessionId, userId)) {
            sendError(principal, "PERMISSION_DENIED", "当前播放模式下无权执行此操作");
            return;
        }

        broadcastPlaybackState(sessionId, payload, "seek", principal);
    }

    @MessageMapping("/playback.next")
    public void handleNext(Map<String, Object> payload, Principal principal) {
        Long userId = extractUserId(principal);
        Long sessionId = toLong(payload.get("sessionId"));

        if (sessionId == null) {
            sendError(principal, "INVALID_MESSAGE", "缺少 sessionId");
            return;
        }

        if (!playbackSyncService.canControl(sessionId, userId)) {
            sendError(principal, "PERMISSION_DENIED", "当前播放模式下无权执行切歌");
            return;
        }

        // Broadcast track switch with buffer window info
        Map<String, Object> state = new HashMap<>(payload);
        state.put("type", "playback.next");
        state.put("operatorId", userId);
        state.put("operatorNickname", extractNickname(principal));
        state.put("serverTimestamp", System.currentTimeMillis());
        state.put("bufferMs", playbackSyncService.getSwitchBufferMs());

        // Need to determine groupId from session
        messagingTemplate.convertAndSend("/topic/session/" + sessionId, state);
    }

    private void broadcastPlaybackState(Long sessionId, Map<String, Object> payload,
                                         String action, Principal principal) {
        Map<String, Object> state = new HashMap<>(payload);
        state.put("type", "playback.state");
        state.put("action", action);
        state.put("playing", "play".equals(action));
        state.put("operatorId", extractUserId(principal));
        state.put("operatorNickname", extractNickname(principal));
        state.put("serverTimestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/session/" + sessionId, state);
    }

    private Long extractUserId(Principal principal) {
        if (principal instanceof WsAuthInterceptor.StompPrincipal p) {
            return p.userId();
        }
        return Long.parseLong(principal.getName());
    }

    private String extractNickname(Principal principal) {
        if (principal instanceof WsAuthInterceptor.StompPrincipal p) {
            return p.username();
        }
        return principal.getName();
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
