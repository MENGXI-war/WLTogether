package com.wltogether.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DeviceService {

    private static final int CERT_VALIDITY_DAYS = 7;
    private static final long PEER_TTL_SECONDS = 30;
    private static final long CLEANUP_INTERVAL_MS = 15_000;

    // Server Ed25519 key pair (generated at startup, memory only)
    private KeyPair serverKeyPair;

    // Peer registry: groupId -> Map<deviceId, PeerInfo>
    private final Map<String, Map<String, PeerEntry>> peerRegistry = new ConcurrentHashMap<>();

    // Rate limiting: IP -> count per hour
    private final Map<String, List<Long>> ipRateLimit = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_HOUR = 10;

    @PostConstruct
    public void init() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("Ed25519");
            serverKeyPair = gen.generateKeyPair();
            log.info("Server Ed25519 key pair generated for device certificates");
        } catch (NoSuchAlgorithmException e) {
            log.error("Ed25519 not available on this JVM", e);
            throw new RuntimeException("Ed25519 not available", e);
        }

        // Start cleanup thread for peer registry
        Thread cleanup = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(CLEANUP_INTERVAL_MS);
                    cleanupExpiredPeers();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "device-peer-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }

    /**
     * Issue a device certificate signed by the server's Ed25519 key.
     */
    public Map<String, Object> issueCertificate(String deviceId, String publicKeyBase64) {
        validateDeviceId(deviceId);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(CERT_VALIDITY_DAYS, ChronoUnit.DAYS);

        // Build certificate payload
        String payload = deviceId + "|" + publicKeyBase64 + "|" + now.toString() + "|" + expiresAt.toString();
        String signature;
        try {
            Signature sig = Signature.getInstance("Ed25519");
            sig.initSign(serverKeyPair.getPrivate());
            sig.update(payload.getBytes(StandardCharsets.UTF_8));
            signature = Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) {
            log.error("Failed to sign device certificate", e);
            throw new RuntimeException("Failed to sign certificate");
        }

        Map<String, Object> cert = new LinkedHashMap<>();
        cert.put("deviceId", deviceId);
        cert.put("publicKey", publicKeyBase64);
        cert.put("issuedAt", now.toString());
        cert.put("expiresAt", expiresAt.toString());
        cert.put("signature", signature);

        log.info("Device certificate issued for {} (expires {})", deviceId, expiresAt);
        return cert;
    }

    /**
     * Verify a device certificate's signature and expiry.
     */
    public boolean verifyCertificate(Map<String, Object> cert) {
        try {
            String deviceId = (String) cert.get("deviceId");
            String publicKey = (String) cert.get("publicKey");
            String issuedAt = (String) cert.get("issuedAt");
            String expiresAt = (String) cert.get("expiresAt");
            String signature = (String) cert.get("signature");

            // Check expiry
            Instant exp = Instant.parse(expiresAt);
            if (Instant.now().isAfter(exp)) {
                return false;
            }

            // Verify signature
            String payload = deviceId + "|" + publicKey + "|" + issuedAt + "|" + expiresAt;
            Signature sig = Signature.getInstance("Ed25519");
            sig.initVerify(serverKeyPair.getPublic());
            sig.update(payload.getBytes(StandardCharsets.UTF_8));
            return sig.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            log.warn("Certificate verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Register a peer for rendezvous in a group.
     */
    public void offerPeer(String groupId, Map<String, Object> cert, Map<String, Object> peerInfo) {
        if (!verifyCertificate(cert)) {
            throw new IllegalArgumentException("证书无效或已过期");
        }

        String deviceId = (String) cert.get("deviceId");
        peerRegistry.computeIfAbsent(groupId, k -> new ConcurrentHashMap<>())
                .put(deviceId, new PeerEntry(cert, peerInfo, Instant.now()));
    }

    /**
     * Query peers in a group. Returns all peers except the requesting device.
     */
    public List<Map<String, Object>> queryPeers(String groupId, Map<String, Object> cert) {
        if (!verifyCertificate(cert)) {
            throw new IllegalArgumentException("证书无效或已过期");
        }

        String deviceId = (String) cert.get("deviceId");
        Map<String, PeerEntry> peers = peerRegistry.getOrDefault(groupId, Collections.emptyMap());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, PeerEntry> entry : peers.entrySet()) {
            if (!entry.getKey().equals(deviceId)) {
                Map<String, Object> peer = new LinkedHashMap<>();
                peer.put("cert", entry.getValue().cert());
                peer.put("peerInfo", entry.getValue().peerInfo());
                result.add(peer);
            }
        }
        return result;
    }

    /**
     * Check and update rate limit for an IP address.
     */
    public boolean checkRateLimit(String ip) {
        List<Long> timestamps = ipRateLimit.computeIfAbsent(ip, k -> new ArrayList<>());
        synchronized (timestamps) {
            Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
            timestamps.removeIf(ts -> Instant.ofEpochMilli(ts).isBefore(oneHourAgo));
            if (timestamps.size() >= MAX_REQUESTS_PER_HOUR) {
                return false;
            }
            timestamps.add(System.currentTimeMillis());
            return true;
        }
    }

    private void cleanupExpiredPeers() {
        Instant cutoff = Instant.now().minusSeconds(PEER_TTL_SECONDS);
        for (Map.Entry<String, Map<String, PeerEntry>> group : peerRegistry.entrySet()) {
            group.getValue().values().removeIf(entry -> entry.registeredAt().isBefore(cutoff));
        }
        peerRegistry.values().removeIf(Map::isEmpty);
    }

    private void validateDeviceId(String deviceId) {
        if (deviceId == null || !deviceId.matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException("无效的 deviceId 格式（需为 64 位 hex 字符串）");
        }
    }

    // ---- inner record ----

    private record PeerEntry(Map<String, Object> cert, Map<String, Object> peerInfo, Instant registeredAt) {}
}
