package com.wltogether.websocket;

import com.wltogether.repository.GroupMemberRepository;
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
public class P2PSignalingHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final GroupMemberRepository groupMemberRepository;

    @MessageMapping("/p2p.signal")
    public void handleSignal(Map<String, Object> payload, Principal principal) {
        Long fromUserId = extractUserId(principal);
        Long targetUserId = toLong(payload.get("targetUserId"));
        Long sessionId = toLong(payload.get("sessionId"));

        if (targetUserId == null || sessionId == null) {
            sendError(principal, "INVALID_MESSAGE", "缺少 targetUserId 或 sessionId");
            return;
        }

        // Verify both users share at least one group
        if (!hasCommonGroup(fromUserId, targetUserId)) {
            sendError(principal, "PERMISSION_DENIED", "目标用户不在同一群组内");
            return;
        }

        // Relay signal to target user
        Map<String, Object> signal = new HashMap<>(payload);
        signal.put("type", "p2p.signal");
        signal.put("fromUserId", fromUserId);

        messagingTemplate.convertAndSendToUser(
                targetUserId.toString(), "/queue/signal", signal);
    }

    private boolean hasCommonGroup(Long userId1, Long userId2) {
        // Users share a group if they are both members of at least one common group
        var groups1 = groupMemberRepository.findByUserId(userId1);
        var groups2 = groupMemberRepository.findByUserId(userId2);

        return groups1.stream().anyMatch(g1 ->
                groups2.stream().anyMatch(g2 -> g2.getGroupId().equals(g1.getGroupId())));
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
