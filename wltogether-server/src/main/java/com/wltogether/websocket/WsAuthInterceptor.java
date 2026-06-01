package com.wltogether.websocket;

import com.wltogether.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (!StringUtils.hasText(token)) {
                log.warn("WebSocket CONNECT rejected: no token");
                throw new IllegalArgumentException("AUTH_INVALID: 请先登录");
            }

            try {
                Claims claims = jwtTokenProvider.validateAccessToken(token);
                Long userId = jwtTokenProvider.getUserId(claims);
                String username = claims.get("username", String.class);

                // Set authenticated principal
                accessor.setUser(new StompPrincipal(userId, username));
                accessor.getSessionAttributes().put("userId", userId);
                accessor.getSessionAttributes().put("username", username);

                log.debug("WebSocket authenticated: userId={}, username={}", userId, username);
            } catch (JwtException e) {
                log.warn("WebSocket CONNECT rejected: invalid token — {}", e.getMessage());
                throw new IllegalArgumentException("AUTH_INVALID: Token 无效或已过期");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // First try query parameter: /ws?token=xxx
        String query = accessor.getFirstNativeHeader("token");
        if (StringUtils.hasText(query)) {
            return query;
        }

        // Then try Authorization header
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearer = authHeaders.get(0);
            if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
                return bearer.substring(7);
            }
        }

        return null;
    }

    /**
     * Simple Principal implementation holding userId and username.
     */
    public record StompPrincipal(Long userId, String username) implements Principal {
        @Override
        public String getName() {
            return username;
        }
    }
}
