package com.blog.ai.controller;

import com.blog.ai.dto.ChatMessage;
import com.blog.ai.service.DeepSeekService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 对外提供 DeepSeek 对话能力（示例接口，可按需加鉴权与限流）。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final DeepSeekService deepSeekService;

    /**
     * 对话补全，非流式。
     * Body: { "messages": [ {"role": "system", "content": "..."}, {"role": "user", "content": "..."} ], "model": "deepseek-chat" (可选) }
     */
    @PostMapping("/chat/completions")
    public ResponseEntity<Map<String, String>> chatCompletions(@RequestBody ChatRequest req) {
        List<ChatMessage> messages = req.getMessages();
        if (messages == null || messages.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String model = req.getModel();
        String content = deepSeekService.chat(messages, model, false);
        return ResponseEntity.ok(Map.of("content", content != null ? content : ""));
    }

    @lombok.Data
    public static class ChatRequest {
        private List<ChatMessage> messages;
        private String model;
    }
}
