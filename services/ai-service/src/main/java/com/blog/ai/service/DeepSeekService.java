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
        // 控制台输出发往大模型的请求内容，便于排查
        log.info("========== 请求大模型 ========== url={}, model={}", url, body.getModel());
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            String role = m != null ? m.getRole() : null;
            String content = m != null ? m.getContent() : null;
            int len = content != null ? content.length() : 0;
            log.info("  [{}] role={}, contentLength={}", i, role, len);
            if (content != null && !content.isBlank()) {
                String preview = content.length() > 500 ? content.substring(0, 500) + "..." : content;
                log.info("  [{}] contentPreview:\n{}", i, preview);
            }
        }
        log.info("========================================");

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

    private static final int SUMMARY_MAX_LENGTH = 100;

    /**
     * 根据正文生成文章摘要，最多 100 个字符。仅可传入摘要风格，不传其他 bot 信息。
     * @param summaryStyle 可选，concise/detailed/quote，仅此风格会作为 prompt 给模型
     */
    public String generateSummaryFromBody(String body, String summaryStyle) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("正文不能为空");
        }
        String input = body.length() > TITLE_BODY_MAX_LENGTH ? body.substring(0, TITLE_BODY_MAX_LENGTH) : body;
        String styleHint = "";
        if (summaryStyle != null && !summaryStyle.isBlank()) {
            String label = switch (summaryStyle.trim()) {
                case "detailed" -> "详细";
                case "quote" -> "金句提炼";
                default -> "简洁";
            };
            styleHint = "摘要风格要求：" + label + "。";
        }
        String userMsg = styleHint.isEmpty()
                ? "请根据以下正文生成摘要：\n\n" + input
                : styleHint + "\n\n请根据以下正文生成摘要：\n\n" + input;
        List<ChatMessage> messages = List.of(
                new ChatMessage("system",
                        "你是一个博客助手。根据用户给出的正文内容，生成一则简短的文章摘要。要求：仅输出摘要文字，不要引号、不要解释、不要换行，最多 100 个字符，使用中文。"),
                new ChatMessage("user", userMsg)
        );
        String raw = chat(messages);
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String summary = raw.replace("\"", "").replace("\n", " ").trim();
        if (summary.length() > SUMMARY_MAX_LENGTH) {
            summary = summary.substring(0, SUMMARY_MAX_LENGTH);
        }
        return summary;
    }

    private static final int TAGS_MAX_COUNT = 5;

    public List<String> generateTagNamesFromBody(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("正文不能为空");
        }
        String input = body.length() > TITLE_BODY_MAX_LENGTH ? body.substring(0, TITLE_BODY_MAX_LENGTH) : body;
        List<ChatMessage> messages = List.of(
                new ChatMessage("system",
                        "你是一个博客助手。根据用户给出的正文内容，生成 3～5 个文章标签，用于分类。要求：仅输出标签，每行一个，不要编号、不要解释，使用中文或英文，最多 5 个。"),
                new ChatMessage("user", "请根据以下正文生成标签（每行一个）：\n\n" + input)
        );
        String raw = chat(messages);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> names = new java.util.ArrayList<>();
        for (String line : raw.split("\n")) {
            String t = line.replaceAll("^[\\d.、\\s]+", "").trim();
            if (!t.isEmpty() && t.length() <= 32) {
                names.add(t);
                if (names.size() >= TAGS_MAX_COUNT) break;
            }
        }
        return names;
    }

    private static final int COVER_PROMPT_MAX_LENGTH = 500;

    /**
     * 根据正文生成适合文生图的封面描述（1～2 句话），用于 Z-Image 等模型。
     */
    public String generateCoverPromptFromBody(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("正文不能为空");
        }
        String input = body.length() > TITLE_BODY_MAX_LENGTH ? body.substring(0, TITLE_BODY_MAX_LENGTH) : body;
        List<ChatMessage> messages = List.of(
                new ChatMessage("system",
                        "你是一个博客助手。根据用户给出的正文内容，生成一句适合作为「文生图」提示词的封面描述。要求：仅输出这一句描述，不要引号、不要解释、不要换行；描述应具象、适合生成一张博客封面图（如场景、风格、色调等），长度 20～200 字，使用中文。"),
                new ChatMessage("user", "请根据以下正文生成封面图描述（一句）：\n\n" + input)
        );
        String raw = chat(messages);
        if (raw == null || raw.isBlank()) {
            return "简洁的博客封面，清新风格";
        }
        String prompt = raw.replace("\"", "").replace("\n", " ").trim();
        if (prompt.length() > COVER_PROMPT_MAX_LENGTH) {
            prompt = prompt.substring(0, COVER_PROMPT_MAX_LENGTH);
        }
        return prompt.isEmpty() ? "简洁的博客封面，清新风格" : prompt;
    }

    private static final int BODY_MAX_LENGTH = 15000;

    /**
     * 根据主题 prompt、发文风格 style 与机器人配置文案 botConfigPrompt 生成博客正文（Markdown）。
     * style: professional / casual / technical / narrative
     * botConfigPrompt: 可选，如「发文风格：xxx，摘要风格：xxx，字数偏好：xxx，主标签：xxx」，会一并作为 prompt 给模型
     */
    public String generateBodyFromPrompt(String prompt, String style, String botConfigPrompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt 不能为空");
        }
        String styleHint = "专业严谨";
        if (style != null) {
            switch (style) {
                case "casual" -> styleHint = "轻松活泼";
                case "technical" -> styleHint = "技术向、偏干货";
                case "narrative" -> styleHint = "故事向、叙述感";
                default -> { /* professional */ }
            }
        }
        StringBuilder userMsg = new StringBuilder();
        if (botConfigPrompt != null && !botConfigPrompt.isBlank()) {
            userMsg.append("请严格遵循以下机器人配置（作为写作约束）：\n").append(botConfigPrompt.trim()).append("\n\n");
        }
        userMsg.append("请根据以下主题生成一篇博客正文（Markdown）：\n\n").append(prompt.trim());

        List<ChatMessage> messages = List.of(
                new ChatMessage("system",
                        "你是一个博客写作助手。根据用户给出的主题或描述，生成一篇博客正文。\n\n"
                        + "格式要求（必须严格遵守，否则正文无法被正确渲染）：\n"
                        + "1. 使用标准 Markdown：标题用 #（一级）、##（二级）、###（三级）等，不要写 H1、H2 等字样。\n"
                        + "2. 段落之间用空行分隔；列表用 - 或 1. ；代码用 ``` 包裹并注明语言；加粗用 **，斜体用 *。\n"
                        + "3. 数学公式必须用 LaTeX：行内公式用 $公式$，独立公式块用 $$公式$$（例如 $\\\\frac{\\\\partial L}{\\\\partial x_l}$）。不要用其他公式语法。\n"
                        + "4. 不要输出任何 HTML 标签、不要用 XML/自定义标签；仅输出可被标准 Markdown 渲染的纯文本。\n"
                        + "文风" + styleHint + "；仅输出正文内容，不要解释、不要前缀说明；长度适中，适合博客阅读。若用户提供了机器人配置，请按配置中的字数偏好与风格写作。"),
                new ChatMessage("user", userMsg.toString())
        );
        String raw = chat(messages);
        if (raw == null || raw.isBlank()) {
            return "# " + prompt.trim() + "\n\n请根据主题继续完善内容。";
        }
        String body = raw.trim();
        if (body.length() > BODY_MAX_LENGTH) {
            body = body.substring(0, BODY_MAX_LENGTH);
        }
        return body;
    }
}
