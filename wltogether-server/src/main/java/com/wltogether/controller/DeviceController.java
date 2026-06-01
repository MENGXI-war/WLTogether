package com.wltogether.controller;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.service.DeviceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * Issue a device certificate. No authentication required.
     * Rate limited: 10 req/hour/IP.
     */
    @PostMapping("/certificate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> issueCertificate(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {

        String ip = getClientIp(request);
        if (!deviceService.checkRateLimit(ip)) {
            return ResponseEntity.status(429)
                    .body(ApiResponse.error("RATE_LIMITED", "请求过于频繁，请稍后重试"));
        }

        try {
            String deviceId = (String) body.get("deviceId");
            String publicKey = (String) body.get("publicKey");
            Map<String, Object> cert = deviceService.issueCertificate(deviceId, publicKey);
            return ResponseEntity.ok(ApiResponse.ok("ok", cert));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", e.getMessage()));
        }
    }

    /**
     * Submit peer info for group rendezvous.
     */
    @PostMapping("/rendezvous/offer")
    public ResponseEntity<ApiResponse<Void>> offer(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {

        String ip = getClientIp(request);
        if (!deviceService.checkRateLimit(ip)) {
            return ResponseEntity.status(429)
                    .body(ApiResponse.error("RATE_LIMITED", "请求过于频繁"));
        }

        try {
            String groupId = (String) body.get("groupId");
            @SuppressWarnings("unchecked")
            Map<String, Object> cert = (Map<String, Object>) body.get("cert");
            @SuppressWarnings("unchecked")
            Map<String, Object> peerInfo = (Map<String, Object>) body.get("peerInfo");

            deviceService.offerPeer(groupId, cert, peerInfo);
            return ResponseEntity.ok(ApiResponse.ok("peer 已登记"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", e.getMessage()));
        }
    }

    /**
     * Query peers in a group.
     */
    @PostMapping("/rendezvous/query")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> query(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {

        String ip = getClientIp(request);
        if (!deviceService.checkRateLimit(ip)) {
            return ResponseEntity.status(429)
                    .body(ApiResponse.error("RATE_LIMITED", "请求过于频繁"));
        }

        try {
            String groupId = (String) body.get("groupId");
            @SuppressWarnings("unchecked")
            Map<String, Object> cert = (Map<String, Object>) body.get("cert");

            List<Map<String, Object>> peers = deviceService.queryPeers(groupId, cert);
            return ResponseEntity.ok(ApiResponse.ok("ok", peers));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", e.getMessage()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
