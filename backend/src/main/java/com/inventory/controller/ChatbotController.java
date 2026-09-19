package com.inventory.controller;

import com.inventory.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/open")
    public ResponseEntity<Map<String, Object>> open() {
        return ResponseEntity.ok(chatbotService.openBotChat());
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", body.getOrDefault("text", ""));
        return ResponseEntity.ok(chatbotService.ask(message));
    }
}
