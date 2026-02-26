package com.blog.ai.controller;

import com.blog.ai.service.DeepSeekService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 根据正文生成博客标题等 AI 能力接口。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiTitleController {

    private final DeepSeekService deepSeekService;

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
}
