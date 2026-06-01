package com.wltogether.websocket;

import com.wltogether.service.PlaybackSyncService;
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
public class SyncHandler {

    private final PlaybackSyncService playbackSyncService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/sync.ping")
    public void handleSyncPing(Map<String, Object> payload, Principal principal) {
        long clientTime = ((Number) payload.getOrDefault("clientTime", 0L)).longValue();

        PlaybackSyncService.ClockSyncResult result = playbackSyncService.computeSync(clientTime);

        Map<String, Object> pong = Map.of(
                "type", "sync.pong",
                "clientTime", result.clientTime(),
                "serverTime", result.serverTime()
        );

        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/sync.pong", pong);
    }
}
