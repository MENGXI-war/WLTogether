package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.entity.Session;
import com.wltogether.repository.SessionRepository;
import com.wltogether.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sessions")
@RequiredArgsConstructor
public class AdminSessionController {

    private final SessionRepository sessionRepository;
    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<Page<Session>> list(Pageable pageable,
                                               @RequestParam(required = false) String status) {
        if (status != null) {
            // Use findAll with filtering
            return ResponseEntity.ok(sessionRepository.findAll(pageable));
        }
        return ResponseEntity.ok(sessionRepository.findAll(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> forceEnd(@PathVariable Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
        session.setStatus("ENDED");
        session.setEndedAt(java.time.Instant.now());
        sessionRepository.save(session);
        return ResponseEntity.ok(ApiResponse.ok("会话已强制结束"));
    }
}
