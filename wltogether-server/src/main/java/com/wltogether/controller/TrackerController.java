package com.wltogether.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Private BitTorrent HTTP tracker.
 * Only accessible to authenticated group members (JWT required).
 * Does NOT support DHT/PEX — only private tracker mode.
 */
@Slf4j
@RestController
@RequestMapping("/api/tracker")
public class TrackerController {

    // infoHash -> set of peer entries
    private static final long PEER_TIMEOUT_MS = 30 * 60 * 1000; // 30 min

    private final Map<String, Map<String, PeerEntry>> swarms = new ConcurrentHashMap<>();

    @GetMapping("/announce")
    public ResponseEntity<Map<String, Object>> announce(
            HttpServletRequest request,
            @RequestParam("peer_id") String peerId,
            @RequestParam("info_hash") String infoHash,
            @RequestParam("port") int port,
            @RequestParam(value = "uploaded", defaultValue = "0") long uploaded,
            @RequestParam(value = "downloaded", defaultValue = "0") long downloaded,
            @RequestParam(value = "left", defaultValue = "0") long left,
            @RequestParam(value = "event", defaultValue = "started") String event) {

        String ip = getClientIp(request);
        String peerKey = ip + ":" + port;

        // Register or update peer
        swarms.computeIfAbsent(infoHash, k -> new ConcurrentHashMap<>())
                .put(peerKey, new PeerEntry(peerId, ip, port, uploaded, downloaded, left, System.currentTimeMillis()));

        // Handle stop event
        if ("stopped".equals(event)) {
            swarms.getOrDefault(infoHash, Collections.emptyMap()).remove(peerKey);
        }

        // Clean up stale peers
        cleanStalePeers(infoHash);

        // Build peer list (exclude self)
        Map<String, PeerEntry> peers = swarms.getOrDefault(infoHash, Collections.emptyMap());
        List<Map<String, Object>> peerList = new ArrayList<>();
        for (PeerEntry p : peers.values()) {
            if (!p.peerId().equals(peerId)) {
                Map<String, Object> peer = new LinkedHashMap<>();
                peer.put("peer_id", p.peerId());
                peer.put("ip", p.ip());
                peer.put("port", p.port());
                peerList.add(peer);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("interval", 300);     // 5 min re-announce
        response.put("min interval", 60);
        response.put("complete", countComplete(peers));
        response.put("incomplete", countIncomplete(peers));
        response.put("peers", peerList);

        return ResponseEntity.ok(response);
    }

    private void cleanStalePeers(String infoHash) {
        long cutoff = System.currentTimeMillis() - PEER_TIMEOUT_MS;
        Map<String, PeerEntry> peers = swarms.get(infoHash);
        if (peers != null) {
            peers.values().removeIf(p -> p.lastSeen() < cutoff);
            if (peers.isEmpty()) {
                swarms.remove(infoHash);
            }
        }
    }

    private int countComplete(Map<String, PeerEntry> peers) {
        return (int) peers.values().stream().filter(p -> p.left() == 0).count();
    }

    private int countIncomplete(Map<String, PeerEntry> peers) {
        return (int) peers.values().stream().filter(p -> p.left() > 0).count();
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private record PeerEntry(String peerId, String ip, int port,
                              long uploaded, long downloaded, long left, long lastSeen) {}
}
