package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.entity.ServerConfig;
import com.wltogether.repository.ServerConfigRepository;
import com.wltogether.service.ServerConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final ServerConfigRepository serverConfigRepository;
    private final ServerConfigService serverConfigService;

    /** Config keys that admins are allowed to modify via the admin panel */
    private static final Set<String> WRITABLE_KEYS = Set.of(
            "transfer.server_relay.enabled",
            "transfer.server_relay.max_file_mb",
            "session.max_participants",
            "session.max_concurrent_per_user"
    );

    @GetMapping
    public ResponseEntity<List<ServerConfig>> list() {
        return ResponseEntity.ok(serverConfigRepository.findAll());
    }

    @GetMapping("/{key}")
    public ResponseEntity<ServerConfig> get(@PathVariable String key) {
        ServerConfig config = serverConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new IllegalArgumentException("配置项不存在: " + key));
        return ResponseEntity.ok(config);
    }

    @PutMapping("/{key}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> update(Authentication auth,
                                                     @PathVariable String key,
                                                     @RequestBody Map<String, String> body) {
        if (!WRITABLE_KEYS.contains(key)) {
            throw new IllegalArgumentException("该配置项不允许通过管理后台修改: " + key);
        }
        Long userId = (Long) auth.getPrincipal();
        String value = body.get("value");
        serverConfigService.setValue(key, value, userId);
        return ResponseEntity.ok(ApiResponse.ok("配置已更新"));
    }
}
