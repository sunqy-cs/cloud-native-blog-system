package com.blog.ai.controller;

import com.blog.ai.entity.Bot;
import com.blog.ai.entity.Tag;
import com.blog.ai.mapper.BotMapper;
import com.blog.ai.mapper.TagMapper;
import com.blog.ai.service.DeepSeekService;
import com.blog.ai.service.ZImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * 根据正文生成博客标题等 AI 能力接口，以及文生图等。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiTitleController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final DeepSeekService deepSeekService;
    private final ZImageService zImageService;
    private final BotMapper botMapper;
    private final TagMapper tagMapper;

    /**
     * 根据正文生成博客标题。需认证（由网关校验）。
     */
    @PostMapping("/title")
    public ResponseEntity<Map<String, String>> generateTitle(@RequestBody TitleRequest req) {
        String body = req != null && req.getBody() != null ? req.getBody().trim() : "";
        if (body.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String title = deepSeekService.generateTitleFromBody(body);
        return ResponseEntity.ok(Map.of("title", title != null ? title : ""));
    }

    /**
     * 根据正文生成文章摘要，上限 100 字。需认证（由网关校验）。
     */
    @PostMapping("/summary")
    public ResponseEntity<Map<String, String>> generateSummary(@RequestBody SummaryRequest req) {
        String body = req != null && req.getBody() != null ? req.getBody().trim() : "";
        if (body.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String summary = deepSeekService.generateSummaryFromBody(body, req.getSummaryStyle());
        return ResponseEntity.ok(Map.of("summary", summary != null ? summary : ""));
    }

    /**
     * 根据正文生成标签，最多 5 个。需认证（由网关校验）。
     */
    @PostMapping("/tags")
    public ResponseEntity<Map<String, Object>> generateTags(@RequestBody TagsRequest req) {
        String body = req != null && req.getBody() != null ? req.getBody().trim() : "";
        if (body.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        java.util.List<String> tagNames = deepSeekService.generateTagNamesFromBody(body);
        return ResponseEntity.ok(Map.of("tagNames", tagNames != null ? tagNames : java.util.List.of()));
    }

    /**
     * 文生图（Z-Image）：根据 prompt 生成图片并存入 MinIO，返回本站 URL。需认证（由网关校验）。
     */
    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> generateImage(@RequestBody ImageRequest req) {
        String prompt = req != null ? req.getPrompt() : null;
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String size = req != null && req.getSize() != null ? req.getSize().trim() : null;
        String url = zImageService.generateImage(prompt, size);
        return ResponseEntity.ok(Map.of("url", url != null ? url : ""));
    }

    /**
     * 根据正文生成封面图：先用大模型生成封面描述，再文生图并存入 MinIO，返回本站 URL。需认证。
     */
    @PostMapping("/cover")
    public ResponseEntity<Map<String, String>> generateCover(@RequestBody CoverRequest req) {
        String body = req != null ? req.getBody() : null;
        if (body == null || body.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String prompt = deepSeekService.generateCoverPromptFromBody(body.trim());
        // 与前端博客封面展示比例一致：120:84 / 200:140 = 10:7
        String url = zImageService.generateImage(prompt, "1120*784");
        return ResponseEntity.ok(Map.of("url", url != null ? url : ""));
    }

    /**
     * 一键生成：根据 bot 与 prompt 生成正文、标题、摘要、小标签、封面、主标签。需认证，请求头 X-User-Id；仅能使用当前用户的 bot。
     */
    @PostMapping("/one-click-generate")
    public ResponseEntity<Map<String, Object>> oneClickGenerate(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestBody OneClickRequest req) {
        if (userId == null || req == null || req.getBotId() == null) {
            return ResponseEntity.badRequest().build();
        }
        String prompt = req.getPrompt() != null ? req.getPrompt().trim() : "";
        if (prompt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Bot bot = botMapper.selectById(req.getBotId());
        if (bot == null || !userId.equals(bot.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "机器人不存在或无权使用");
        }

        String botConfigPrompt = buildBotConfigPrompt(bot);
        String body = deepSeekService.generateBodyFromPrompt(prompt, bot.getStyle(), botConfigPrompt);
        String bodyForMeta = body.length() > 2000 ? body.substring(0, 2000) : body;
        String title = deepSeekService.generateTitleFromBody(bodyForMeta);

        String summary = "";
        try {
            summary = deepSeekService.generateSummaryFromBody(bodyForMeta, null);
            if (summary == null) summary = "";
        } catch (Exception e) {
            // 摘要生成失败不阻塞
        }

        java.util.List<String> tagNames = java.util.List.of();
        try {
            tagNames = deepSeekService.generateTagNamesFromBody(bodyForMeta);
            if (tagNames == null) tagNames = java.util.List.of();
        } catch (Exception e) {
            // 小标签生成失败不阻塞
        }

        String coverUrl = null;
        try {
            String coverPrompt = deepSeekService.generateCoverPromptFromBody(bodyForMeta);
            coverUrl = zImageService.generateImage(coverPrompt, "1120*784");
        } catch (Exception e) {
            // 封面生成失败不阻塞，返回 null
        }
        Long mainTagId = bot.getMainTagId();

        Map<String, Object> res = new HashMap<>();
        res.put("body", body != null ? body : "");
        res.put("title", title != null ? title : "");
        res.put("summary", summary);
        res.put("tagNames", tagNames);
        res.put("coverUrl", coverUrl);
        res.put("mainTagId", mainTagId);
        return ResponseEntity.ok(res);
    }

    @lombok.Data
    public static class TitleRequest {
        private String body;
    }

    @lombok.Data
    public static class SummaryRequest {
        private String body;
        /** 可选，摘要风格：concise/detailed/quote，仅此信息会传给模型，不含其他 bot 配置 */
        private String summaryStyle;
    }

    @lombok.Data
    public static class TagsRequest {
        private String body;
    }

    @lombok.Data
    public static class ImageRequest {
        private String prompt;
        private String size;
    }

    @lombok.Data
    public static class CoverRequest {
        private String body;
    }

    /** 将 bot 配置拼成给大模型的 prompt 文案（生成正文用；不含摘要风格，摘要风格仅在生成摘要时传入） */
    private String buildBotConfigPrompt(Bot bot) {
        StringBuilder sb = new StringBuilder();
        String styleLabel = styleLabel(bot.getStyle());
        if (styleLabel != null) sb.append("发文风格：").append(styleLabel).append("。");
        String wordLabel = wordCountLabel(bot.getWordCountPreference());
        if (wordLabel != null) sb.append("字数偏好：").append(wordLabel).append("。");
        if (bot.getMainTagId() != null) {
            Tag tag = tagMapper.selectById(bot.getMainTagId());
            if (tag != null && tag.getName() != null) {
                sb.append("主标签：").append(tag.getName()).append("。");
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private static String styleLabel(String style) {
        if (style == null) return null;
        return switch (style) {
            case "casual" -> "轻松活泼";
            case "technical" -> "技术向";
            case "narrative" -> "故事向";
            default -> "专业严谨";
        };
    }

    private static String wordCountLabel(String w) {
        if (w == null) return null;
        return switch (w) {
            case "short" -> "短篇（约 500～1000 字）";
            case "long" -> "长篇（2000 字以上）";
            default -> "中篇（约 1000～2000 字）";
        };
    }

    @lombok.Data
    public static class OneClickRequest {
        private Long botId;
        private String prompt;
    }
}
