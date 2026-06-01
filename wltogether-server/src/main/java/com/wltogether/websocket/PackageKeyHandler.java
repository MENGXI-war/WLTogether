package com.wltogether.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Controller
public class PackageKeyHandler {

    private final SimpMessagingTemplate messagingTemplate;
    // packageHash -> {key, exporterId, expiresAt}
    private final Map<String, KeyEntry> keyStore = new ConcurrentHashMap<>();

    public PackageKeyHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/package.key")
    public void handlePackageKey(Map<String, Object> payload, Principal principal) {
        Long exporterId = extractUserId(principal);
        Long groupId = toLong(payload.get("groupId"));
        String packageHash = (String) payload.get("packageHash");
        String key = (String) payload.get("key");

        if (groupId == null || packageHash == null || key == null) {
            sendError(principal, "INVALID_MESSAGE", "缺少 groupId、packageHash 或 key");
            return;
        }

        // Determine TTL from payload or default to 24 hours
        long ttlSeconds = 86400; // 24h default
        Object expiresIn = payload.get("expiresIn");
        if (expiresIn instanceof Number n) {
            ttlSeconds = n.longValue();
        }

        long expiresAt = System.currentTimeMillis() + ttlSeconds * 1000;
        keyStore.put(packageHash, new KeyEntry(key, exporterId, expiresAt));

        log.info("Offline package key stored: packageHash={}, exporterId={}, ttl={}s",
                packageHash, exporterId, ttlSeconds);

        // Broadcast to all group members
        messagingTemplate.convertAndSend("/topic/group/" + groupId,
                Map.of("type", "package.key",
                       "packageHash", packageHash,
                       "exporterId", exporterId,
                       "key", key,
                       "expiresAt", expiresAt));
    }

    public KeyEntry getKey(String packageHash) {
        KeyEntry entry = keyStore.get(packageHash);
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expiresAt) {
            keyStore.remove(packageHash);
            return null;
        }
        return entry;
    }

    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        keyStore.entrySet().removeIf(e -> e.getValue().expiresAt < now);
    }

    public record KeyEntry(String key, Long exporterId, long expiresAt) {}

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
