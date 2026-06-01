package com.wltogether.service;

import com.wltogether.model.entity.Session;
import com.wltogether.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaybackSyncService {

    private final SessionRepository sessionRepository;

    // sessionId -> playMode (HOST_ONLY, FREE, SHARED_CONTROL)
    private final Map<Long, String> playModes = new ConcurrentHashMap<>();
    // sessionId -> userId -> last seek time (for debounce)
    private final Map<Long, Map<Long, Long>> lastSeekTime = new ConcurrentHashMap<>();

    private static final long SEEK_DEBOUNCE_MS = 3000; // 3 seconds
    private static final long SWITCH_BUFFER_MS = 2000; // 2 seconds

    public String getPlayMode(Long sessionId) {
        return playModes.getOrDefault(sessionId, "HOST_ONLY");
    }

    public void setPlayMode(Long sessionId, String mode) {
        playModes.put(sessionId, mode);
        log.info("Session {} play mode changed to {}", sessionId, mode);
    }

    /**
     * Check if the user is allowed to control playback.
     */
    public boolean canControl(Long sessionId, Long userId) {
        String mode = getPlayMode(sessionId);
        if ("FREE".equals(mode)) {
            // In free mode, control is local only — no broadcast
            return false;
        }
        if ("SHARED_CONTROL".equals(mode)) {
            return !isSeekDebounced(sessionId, userId);
        }
        // HOST_ONLY — only the host can control
        Session session = sessionRepository.findById(sessionId).orElse(null);
        return session != null && session.getHostId().equals(userId);
    }

    /**
     * Rate-limiting for SHARED_CONTROL mode: max 1 seek per 3 seconds per user.
     */
    public boolean isSeekDebounced(Long sessionId, Long userId) {
        long now = System.currentTimeMillis();
        Map<Long, Long> userTimes = lastSeekTime.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
        Long last = userTimes.get(userId);
        if (last != null && (now - last) < SEEK_DEBOUNCE_MS) {
            return true;
        }
        userTimes.put(userId, now);
        return false;
    }

    /**
     * Get the switch buffer window in milliseconds.
     * In music mode, a 2-second buffer window is used before starting the next track.
     */
    public long getSwitchBufferMs() {
        return SWITCH_BUFFER_MS;
    }

    /**
     * Compute clock offset. Client sends t1, server records t2.
     * Client will compute: offset = t2 - (t1 + (t3 - t1) / 2)
     */
    public ClockSyncResult computeSync(long clientTime) {
        long serverTime = System.currentTimeMillis();
        return new ClockSyncResult(clientTime, serverTime);
    }

    public record ClockSyncResult(long clientTime, long serverTime) {}

    public void cleanupSession(Long sessionId) {
        playModes.remove(sessionId);
        lastSeekTime.remove(sessionId);
    }
}
