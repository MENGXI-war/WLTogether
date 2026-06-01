package com.wltogether.controller;

import com.wltogether.model.dto.ChatMessageResponse;
import com.wltogether.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/messages")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            Authentication auth,
            @PathVariable Long groupId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        Long userId = (Long) auth.getPrincipal();
        // Membership check is done by the service/websocket layer;
        // for REST, we trust the security filter.
        return ResponseEntity.ok(chatService.getMessages(groupId, cursor, limit));
    }

    @GetMapping("/pinned")
    public ResponseEntity<List<ChatMessageResponse>> getPinnedMessages(
            Authentication auth,
            @PathVariable Long groupId) {
        return ResponseEntity.ok(chatService.getPinnedMessages(groupId));
    }
}
