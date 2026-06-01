package com.wltogether.controller;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.dto.CreateSessionRequest;
import com.wltogether.model.dto.SessionResponse;
import com.wltogether.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/api/groups/{groupId}/sessions")
    public ResponseEntity<SessionResponse> create(Authentication auth,
                                                   @PathVariable Long groupId,
                                                   @Valid @RequestBody CreateSessionRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.createSession(groupId, userId, request));
    }

    @GetMapping("/api/groups/{groupId}/sessions")
    public ResponseEntity<List<SessionResponse>> list(Authentication auth, @PathVariable Long groupId) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(sessionService.listSessions(groupId, userId));
    }

    @GetMapping("/api/sessions/{id}")
    public ResponseEntity<SessionResponse> get(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(sessionService.getSession(id, userId));
    }

    @PostMapping("/api/sessions/{id}/join")
    public ResponseEntity<SessionResponse> join(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(sessionService.joinSession(id, userId));
    }

    @PostMapping("/api/sessions/{id}/end")
    public ResponseEntity<ApiResponse<Void>> end(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        sessionService.endSession(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("会话已结束"));
    }
}
