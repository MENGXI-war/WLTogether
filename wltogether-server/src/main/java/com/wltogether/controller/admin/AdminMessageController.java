package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.entity.ChatMessage;
import com.wltogether.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/messages")
@RequiredArgsConstructor
public class AdminMessageController {

    private final ChatMessageRepository chatMessageRepository;

    @GetMapping
    public ResponseEntity<Page<ChatMessage>> list(Pageable pageable,
                                                   @RequestParam(required = false) Long groupId,
                                                   @RequestParam(required = false) Long senderId) {
        // For simplicity, return all messages with pagination
        return ResponseEntity.ok(chatMessageRepository.findAll(pageable));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        chatMessageRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("消息已删除"));
    }
}
