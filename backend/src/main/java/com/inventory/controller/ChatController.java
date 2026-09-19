package com.inventory.controller;

import com.inventory.dto.ChatMessageRequest;
import com.inventory.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> conversations() {
        return ResponseEntity.ok(chatService.listConversations());
    }

    @PostMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getOrCreate(@RequestParam Long otherUserId) {
        return ResponseEntity.ok(chatService.getOrCreateConversation(otherUserId));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<Map<String, Object>>> messages(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.listMessages(id));
    }

    @PostMapping("/messages")
    public ResponseEntity<Map<String, Object>> sendText(@Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatService.sendText(request));
    }

    @PostMapping("/messages/voice")
    public ResponseEntity<Map<String, Object>> sendVoice(@Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatService.sendVoice(request));
    }
}
