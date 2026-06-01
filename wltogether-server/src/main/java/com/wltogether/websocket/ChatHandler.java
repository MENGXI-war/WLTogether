package com.wltogether.websocket;

import com.wltogether.model.dto.ChatMessageRequest;
import com.wltogether.model.dto.ChatMessageResponse;
import com.wltogether.repository.GroupMemberRepository;
import com.wltogether.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatHandler {

    private final ChatService chatService;
    private final GroupMemberRepository groupMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void handleChatSend(ChatMessageRequest request, Principal principal) {
        Long userId = extractUserId(principal);
        log.debug("chat.send from userId={}, groupId={}", userId, request.getGroupId());

        // Validate group membership
        if (!groupMemberRepository.existsByGroupIdAndUserId(request.getGroupId(), userId)) {
            sendError(principal, "GROUP_NOT_MEMBER", "你不在此群组中");
            return;
        }

        // Persist and broadcast
        ChatMessageResponse response = chatService.saveMessage(userId, request);
        messagingTemplate.convertAndSend("/topic/group/" + request.getGroupId(), response);
    }

    private Long extractUserId(Principal principal) {
        if (principal instanceof WsAuthInterceptor.StompPrincipal p) {
            return p.userId();
        }
        return Long.parseLong(principal.getName());
    }

    private void sendError(Principal principal, String code, String message) {
        var error = java.util.Map.of("type", "error", "code", code, "message", message);
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/error", error);
    }
}
