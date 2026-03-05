package com.blog.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.ai.client.RagSearchClient;
import com.blog.ai.dto.ChatMessage;
import com.blog.ai.entity.RagConversation;
import com.blog.ai.entity.RagMessage;
import com.blog.ai.mapper.RagConversationMapper;
import com.blog.ai.mapper.RagMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RAG 问答：检索知识库片段 + DeepSeek 流式回答 + 保存聊天记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private static final int RAG_TOP_K = 8;
    private static final int HISTORY_MESSAGES_LIMIT = 10;
    private static final String SYSTEM_ROLE = "system";
    private static final String USER_ROLE = "user";
    private static final String ASSISTANT_ROLE = "assistant";
    private static final long SSE_TIMEOUT_MS = 120_000;

    private final RagSearchClient ragSearchClient;
    private final DeepSeekService deepSeekService;
    private final RagConversationMapper conversationMapper;
    private final RagMessageMapper messageMapper;

    @Value("${app.deepseek.default-model:deepseek-chat}")
    private String defaultModel;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 流式 RAG 对话：检索上下文、调用 DeepSeek 流式输出、写库。通过 SseEmitter 推给前端。
     * kbIds 为空或 null 时不使用 RAG；否则在指定知识库中检索。
     */
    public SseEmitter chatStream(Long userId, List<Long> kbIds, Long conversationId, String question) {
        if (userId == null || question == null || question.isBlank()) {
            throw new IllegalArgumentException("userId 与 question 必填");
        }
        Long singleKbId = (kbIds != null && kbIds.size() == 1) ? kbIds.get(0) : (kbIds != null && !kbIds.isEmpty() ? kbIds.get(0) : null);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        executor.execute(() -> {
            try {
                RagConversation conv = getOrCreateConversation(userId, singleKbId, conversationId);
                if (conv == null) {
                    sendError(emitter, "会话不存在或无权访问");
                    return;
                }
                List<String> contextChunks = (kbIds != null && !kbIds.isEmpty())
                        ? ragSearchClient.searchWithKbIds(kbIds, question.trim(), RAG_TOP_K)
                        : List.of();
                String systemContent = buildSystemPrompt(contextChunks);
                List<ChatMessage> history = loadHistoryForPrompt(conv.getId(), HISTORY_MESSAGES_LIMIT);
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(new ChatMessage(SYSTEM_ROLE, systemContent));
                for (ChatMessage m : history) {
                    messages.add(m);
                }
                messages.add(new ChatMessage(USER_ROLE, question.trim()));

                saveMessage(conv.getId(), USER_ROLE, question.trim());
                StringBuilder fullContent = new StringBuilder();
                deepSeekService.chatStream(messages, defaultModel, delta -> {
                    fullContent.append(delta);
                    try {
                        emitter.send(SseEmitter.event().data(delta, MediaType.TEXT_PLAIN));
                    } catch (IOException e) {
                        log.warn("RAG 流式推送失败", e);
                    }
                });
                saveMessage(conv.getId(), ASSISTANT_ROLE, fullContent.toString());
                updateConversationTitleIfEmpty(conv, question.trim());
                emitter.send(SseEmitter.event().name("done").data(""));
                emitter.complete();
            } catch (Exception e) {
                log.error("RAG 流式对话异常", e);
                sendError(emitter, e.getMessage());
            }
        });
        return emitter;
    }

    private RagConversation getOrCreateConversation(Long userId, Long kbId, Long conversationId) {
        if (conversationId != null && conversationId > 0) {
            RagConversation c = conversationMapper.selectById(conversationId);
            if (c != null && c.getUserId().equals(userId)) return c;
            return null;
        }
        RagConversation c = new RagConversation();
        c.setUserId(userId);
        c.setKbId(kbId);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        conversationMapper.insert(c);
        return c;
    }

    private String buildSystemPrompt(List<String> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个基于知识库的智能助手。请仅根据以下「参考内容」回答用户问题；若参考内容与问题无关或不足，请如实说明。不要编造未在参考中出现的信息。\n\n");
        if (chunks != null && !chunks.isEmpty()) {
            sb.append("【参考内容】\n");
            for (int i = 0; i < chunks.size(); i++) {
                sb.append(i + 1).append(". ").append(chunks.get(i)).append("\n\n");
            }
        } else {
            sb.append("（当前没有检索到相关参考内容，请根据你的知识简要回答或提示用户补充知识库。）\n\n");
        }
        return sb.toString();
    }

    private List<ChatMessage> loadHistoryForPrompt(Long conversationId, int limit) {
        List<RagMessage> list = messageMapper.selectList(
                new LambdaQueryWrapper<RagMessage>()
                        .eq(RagMessage::getConversationId, conversationId)
                        .orderByDesc(RagMessage::getId)
                        .last("LIMIT " + limit));
        List<ChatMessage> out = new ArrayList<>();
        for (int i = list.size() - 1; i >= 0; i--) {
            RagMessage m = list.get(i);
            if (SYSTEM_ROLE.equals(m.getRole())) continue;
            out.add(new ChatMessage(m.getRole(), m.getContent()));
        }
        return out;
    }

    private void saveMessage(Long conversationId, String role, String content) {
        RagMessage m = new RagMessage();
        m.setConversationId(conversationId);
        m.setRole(role);
        m.setContent(content);
        m.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(m);
    }

    private void updateConversationTitleIfEmpty(RagConversation conv, String firstQuestion) {
        if (conv.getTitle() != null && !conv.getTitle().isBlank()) return;
        String title = firstQuestion.length() > 64 ? firstQuestion.substring(0, 64) + "…" : firstQuestion;
        conv.setTitle(title);
        conv.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conv);
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message != null ? message : "未知错误", MediaType.TEXT_PLAIN));
        } catch (IOException ignored) {
        }
        emitter.completeWithError(new RuntimeException(message));
    }

    public List<RagConversation> listConversations(Long userId, int limit) {
        return conversationMapper.selectList(
                new LambdaQueryWrapper<RagConversation>()
                        .eq(RagConversation::getUserId, userId)
                        .orderByDesc(RagConversation::getUpdatedAt)
                        .last("LIMIT " + Math.min(limit, 50)));
    }

    public List<RagMessage> listMessages(Long userId, Long conversationId, int limit) {
        RagConversation c = conversationMapper.selectById(conversationId);
        if (c == null || !c.getUserId().equals(userId)) return List.of();
        return messageMapper.selectList(
                new LambdaQueryWrapper<RagMessage>()
                        .eq(RagMessage::getConversationId, conversationId)
                        .orderByAsc(RagMessage::getId)
                        .last("LIMIT " + Math.min(limit, 100)));
    }

    /**
     * 删除会话（仅本人）。消息表有 ON DELETE CASCADE，会一并删除。
     */
    public boolean deleteConversation(Long userId, Long conversationId) {
        RagConversation c = conversationMapper.selectById(conversationId);
        if (c == null || !c.getUserId().equals(userId)) return false;
        conversationMapper.deleteById(conversationId);
        return true;
    }
}
