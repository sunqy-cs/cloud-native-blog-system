package com.blog.ai.service;

import com.blog.ai.dto.ChatCompletionRequest;
import com.blog.ai.dto.ChatCompletionResponse;
import com.blog.ai.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekService {

    private final RestTemplate restTemplate;

    @Value("${app.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${app.deepseek.api-key:}")
    private String apiKey;

    @Value("${app.deepseek.default-model:deepseek-chat}")
    private String defaultModel;

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final int TITLE_BODY_MAX_LENGTH = 2000;
    private static final int TITLE_MAX_LENGTH = 100;
    private static final int TITLE_MIN_LENGTH = 5;

    public String chat(List<ChatMessage> messages) {
        return chat(messages, defaultModel, false);
    }

    public String chat(List<ChatMessage> messages, String model, boolean stream) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DeepSeek API Key 未配置，请设置 DEEPSEEK_API_KEY");
            throw new IllegalStateException("DeepSeek API Key 未配置");
        }
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages 不能为空");
        }
        String url = baseUrl.replaceAll("/$", "") + CHAT_COMPLETIONS_PATH;
        ChatCompletionRequest body = ChatCompletionRequest.builder()
                .model(model != null && !model.isBlank() ? model : defaultModel)
                .messages(messages)
                .stream(stream)
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        if (stream) {
            log.warn("当前未实现流式解析，请使用 stream=false");
            return null;
        }
        HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(body, headers);
        ChatCompletionResponse response = restTemplate.postForObject(url, entity, ChatCompletionResponse.class);
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return null;
        }
        ChatCompletionResponse.Choice first = response.getChoices().get(0);
        if (first.getMessage() == null) {
            return null;
        }
        return first.getMessage().getContent();
    }

    public String generateTitleFromBody(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("正文不能为空");
        }
        String input = body.length() > TITLE_BODY_MAX_LENGTH ? body.substring(0, TITLE_BODY_MAX_LENGTH) : body;
        List<ChatMessage> messages = List.of(
                new ChatMessage("system",
                        "你是一个博客助手。根据用户给出的正文内容，生成一则简洁的博客标题。要求：仅输出标题文字，不要引号、不要解释、不要换行，长度 5~100 个字符，使用中文。"),
                new ChatMessage("user", "请根据以下正文生成标题：\n\n" + input)
        );
        String raw = chat(messages);
        if (raw == null || raw.isBlank()) {
            return "未生成标题";
        }
        String title = raw.replace("\"", "").replace("\n", " ").trim();
        if (title.length() > TITLE_MAX_LENGTH) {
            title = title.substring(0, TITLE_MAX_LENGTH);
        }
        if (title.length() < TITLE_MIN_LENGTH && raw.length() >= TITLE_MIN_LENGTH) {
            title = raw.replace("\"", "").replace("\n", " ").trim();
            if (title.length() > TITLE_MAX_LENGTH) title = title.substring(0, TITLE_MAX_LENGTH);
        }
        return title.isEmpty() ? "未生成标题" : title;
    }
}
