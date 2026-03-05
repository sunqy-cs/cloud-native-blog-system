package com.blog.ai.controller;

import com.blog.ai.entity.RagConversation;
import com.blog.ai.entity.RagMessage;
import com.blog.ai.service.RagService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * RAG 问答：流式对话、会话与消息列表。
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final RagService ragService;

    /**
     * 流式 RAG 对话。Body: { "conversationId": 可选, "kbId": 可选, "question": "用户问题" }
     * 返回 SSE 流：每段为纯文本 delta；结束时发送 event=done。
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestBody RagChatRequest req) {
        if (userId == null) {
            throw new IllegalArgumentException("缺少 " + HEADER_USER_ID);
        }
        return ragService.chatStream(
                userId,
                req.getKbIds(),
                req.getConversationId(),
                req.getQuestion());
    }

    /**
     * 当前用户的会话列表，按更新时间倒序。
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<RagConversation>> listConversations(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(ragService.listConversations(userId, limit));
    }

    /**
     * 某会话的消息列表，按时间正序。
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<RagMessage>> listMessages(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "50") int limit) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(ragService.listMessages(userId, conversationId, limit));
    }

    /**
     * 删除会话（仅本人；关联消息由 DB CASCADE 删除）。
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long conversationId) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ragService.deleteConversation(userId, conversationId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @Data
    public static class RagChatRequest {
        private Long conversationId;
        /** 知识库 ID 列表，空或不传表示不使用 RAG */
        private List<Long> kbIds;
        private String question;
    }
}
