package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.entity.ServerConfig;
import com.wltogether.repository.ServerConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final ServerConfigRepository serverConfigRepository;

    @GetMapping
    public ResponseEntity<List<ServerConfig>> list() {
        return ResponseEntity.ok(serverConfigRepository.findAll());
    }

    @PutMapping("/{key}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> update(Authentication auth,
                                                     @PathVariable String key,
                                                     @RequestBody Map<String, String> body) {
        Long userId = (Long) auth.getPrincipal();
        ServerConfig config = serverConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new IllegalArgumentException("配置项不存在: " + key));
        config.setConfigValue(body.get("value"));
        config.setUpdatedBy(userId);
        config.setUpdatedAt(java.time.Instant.now());
        serverConfigRepository.save(config);
        return ResponseEntity.ok(ApiResponse.ok("配置已更新"));
    }
}
