package com.recipereels.controller;

import com.recipereels.dto.ApiResponse;
import com.recipereels.dto.DirectMessageCreateDTO;
import com.recipereels.dto.DirectMessageDTO;
import com.recipereels.security.UserPrincipal;
import com.recipereels.service.InteractionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final InteractionService interactionService;

    public MessageController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> sendMessage(
            @Valid @RequestBody DirectMessageCreateDTO dto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        DirectMessageDTO msg = interactionService.sendMessage(dto, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", msg));
    }

    @GetMapping("/inbox")
    public ResponseEntity<ApiResponse> getInbox(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<DirectMessageDTO> inbox = interactionService.getInbox(userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Inbox retrieved", inbox));
    }

    @GetMapping("/sent")
    public ResponseEntity<ApiResponse> getSentMessages(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<DirectMessageDTO> sent = interactionService.getSentMessages(userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Sent messages retrieved", sent));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse> getUnreadCount(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        long count = interactionService.getUnreadMessageCount(userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Unread count", Map.of("count", count)));
    }
}
