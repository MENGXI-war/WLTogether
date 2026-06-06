package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.entity.Session;
import com.wltogether.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/sessions")
@RequiredArgsConstructor
public class AdminSessionController {

    private final SessionRepository sessionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ResponseEntity<Page<Session>> list(Pageable pageable,
                                               @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(sessionRepository.findByStatusPage(status, pageable));
        }
        return ResponseEntity.ok(sessionRepository.findAll(pageable));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> forceEnd(@PathVariable Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
        session.setStatus("ENDED");
        session.setEndedAt(java.time.Instant.now());
        sessionRepository.save(session);

        // Notify participants via WebSocket
        messagingTemplate.convertAndSend("/topic/session/" + id,
                Map.of("type", "SESSION_ENDED", "sessionId", id, "reason", "管理员强制结束"));

        return ResponseEntity.ok(ApiResponse.ok("会话已强制结束"));
    }
}
