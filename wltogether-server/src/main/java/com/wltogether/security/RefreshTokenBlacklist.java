package com.wltogether.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RefreshTokenBlacklist {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String tokenId, long ttlSeconds) {
        blacklist.put(tokenId, System.currentTimeMillis() + ttlSeconds * 1000);
    }

    public boolean isBlacklisted(String tokenId) {
        Long expiry = blacklist.get(tokenId);
        if (expiry == null) return false;

        if (System.currentTimeMillis() > expiry) {
            blacklist.remove(tokenId);
            return false;
        }
        return true;
    }

    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
