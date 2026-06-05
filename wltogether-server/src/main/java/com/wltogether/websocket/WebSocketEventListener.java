package com.wltogether.websocket;

import com.wltogether.service.OnlineStatusService;
import com.wltogether.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final OnlineStatusService onlineStatusService;
    private final SessionService sessionService;

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = (Long) accessor.getSessionAttributes().get("userId");
        String wsSessionId = accessor.getSessionId();

        if (userId != null && wsSessionId != null) {
            onlineStatusService.userConnected(userId, wsSessionId);
            sessionService.onUserConnected(userId);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String wsSessionId = accessor.getSessionId();
        Long userId = (Long) accessor.getSessionAttributes().get("userId");

        if (wsSessionId != null) {
            onlineStatusService.userDisconnected(wsSessionId);
        }
        if (userId != null) {
            sessionService.onUserDisconnected(userId);
        }
    }
}
