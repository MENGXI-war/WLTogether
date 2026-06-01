package com.wltogether.service;

import com.wltogether.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineStatusService {

    private final GroupMemberRepository groupMemberRepository;

    // userId -> Set of WebSocket sessionIds
    private final Map<Long, Set<String>> onlineUsers = new ConcurrentHashMap<>();
    // wsSessionId -> userId (reverse mapping for cleanup)
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();

    public void userConnected(Long userId, String wsSessionId) {
        onlineUsers.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(wsSessionId);
        sessionToUser.put(wsSessionId, userId);
        log.debug("User {} connected (wsSession={}), total sessions: {}",
                userId, wsSessionId, onlineUsers.get(userId).size());
    }

    public void userDisconnected(String wsSessionId) {
        Long userId = sessionToUser.remove(wsSessionId);
        if (userId != null) {
            Set<String> sessions = onlineUsers.get(userId);
            if (sessions != null) {
                sessions.remove(wsSessionId);
                if (sessions.isEmpty()) {
                    onlineUsers.remove(userId);
                    log.debug("User {} is now offline", userId);
                } else {
                    log.debug("User {} wsSession {} removed, {} remaining",
                            userId, wsSessionId, sessions.size());
                }
            }
        }
    }

    public boolean isOnline(Long userId) {
        Set<String> sessions = onlineUsers.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Get the set of online user IDs in a given group.
     */
    public Set<Long> getOnlineUsersInGroup(Long groupId) {
        Set<Long> groupMembers = groupMemberRepository.findByGroupId(groupId).stream()
                .map(m -> m.getUserId())
                .collect(Collectors.toSet());

        return onlineUsers.keySet().stream()
                .filter(groupMembers::contains)
                .collect(Collectors.toSet());
    }

    /**
     * Get all currently online user IDs.
     */
    public Set<Long> getOnlineUsers() {
        return Collections.unmodifiableSet(onlineUsers.keySet());
    }

    /**
     * Get the count of online users.
     */
    public int onlineCount() {
        return onlineUsers.size();
    }
}
