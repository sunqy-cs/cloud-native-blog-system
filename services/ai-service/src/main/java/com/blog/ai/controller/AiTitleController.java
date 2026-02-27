package com.blog.ai.controller;

import com.blog.ai.service.DeepSeekService;
import com.blog.ai.service.ZImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 根据正文生成博客标题等 AI 能力接口，以及文生图等。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiTitleController {

    private final DeepSeekService deepSeekService;
    private final ZImageService zImageService;

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
        String summary = deepSeekService.generateSummaryFromBody(body);
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

    @lombok.Data
    public static class TitleRequest {
        private String body;
    }

    @lombok.Data
    public static class SummaryRequest {
        private String body;
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
}
