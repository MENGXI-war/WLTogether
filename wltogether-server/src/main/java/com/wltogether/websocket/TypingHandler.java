package com.wltogether.websocket;

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
public class TypingHandler {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/typing.start")
    public void handleTypingStart(Map<String, Object> payload, Principal principal) {
        Long groupId = toLong(payload.get("groupId"));
        Long userId = extractUserId(principal);
        String nickname = extractNickname(principal);

        log.debug("typing.start from userId={}, groupId={}", userId, groupId);
        messagingTemplate.convertAndSend("/topic/group/" + groupId,
                Map.of("type", "typing.status", "groupId", groupId,
                       "userId", userId, "nickname", nickname));
    }

    @MessageMapping("/typing.stop")
    public void handleTypingStop(Map<String, Object> payload, Principal principal) {
        Long groupId = toLong(payload.get("groupId"));
        Long userId = extractUserId(principal);
        String nickname = extractNickname(principal);

        log.debug("typing.stop from userId={}, groupId={}", userId, groupId);
        messagingTemplate.convertAndSend("/topic/group/" + groupId,
                Map.of("type", "typing.status", "groupId", groupId,
                       "userId", userId, "nickname", nickname, "stopped", true));
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
        if (value instanceof String s) return Long.parseLong(s);
        return null;
    }
}
