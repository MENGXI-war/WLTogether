package com.wltogether.controller;

import com.wltogether.model.dto.CapabilitiesResponse;
import com.wltogether.service.ServerConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/server")
@RequiredArgsConstructor
public class ServerController {

    private final ServerConfigService configService;

    @GetMapping("/capabilities")
    public ResponseEntity<CapabilitiesResponse> capabilities() {
        return ResponseEntity.ok(CapabilitiesResponse.builder()
                .version("0.1.0")
                .transferModes(Map.of(
                        "p2pDirect", true,
                        "torrentSeed", true,
                        "offlinePackage", true,
                        "lanTransfer", true,
                        "serverRelay", configService.getBool("transfer.server_relay.enabled", false)
                ))
                .limits(Map.of(
                        "maxFileMb", configService.getInt("transfer.server_relay.max_file_mb", 20480),
                        "maxSessionParticipants", configService.getInt("session.max_participants", 20),
                        "maxConcurrentSessions", 5
                ))
                .build());
    }
}
