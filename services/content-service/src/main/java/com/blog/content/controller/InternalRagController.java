package com.blog.content.controller;

import com.blog.content.service.KbVectorService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内部接口：供 ai-service RAG 问答调用，按问题在知识库中做向量检索。
 */
@RestController
@RequestMapping("/internal/kb")
@RequiredArgsConstructor
public class InternalRagController {

    private static final int DEFAULT_TOP_K = 8;
    private static final int MAX_TOP_K = 20;

    private final KbVectorService kbVectorService;

    @PostMapping("/rag-search")
    public ResponseEntity<RagSearchResponse> ragSearch(@RequestBody RagSearchRequest req) {
        if (req == null || req.getQuery() == null || req.getQuery().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        int topK = req.getTopK() != null && req.getTopK() > 0
                ? Math.min(req.getTopK(), MAX_TOP_K)
                : DEFAULT_TOP_K;
        List<String> chunks;
        if (req.getKbIds() != null && !req.getKbIds().isEmpty()) {
            chunks = kbVectorService.searchChunksMultipleKbs(req.getKbIds(), req.getQuery().trim(), topK);
        } else if (req.getKbId() != null) {
            chunks = kbVectorService.searchChunks(req.getKbId(), req.getQuery().trim(), topK);
        } else {
            chunks = List.of();
        }
        return ResponseEntity.ok(new RagSearchResponse(chunks));
    }

    @Data
    public static class RagSearchRequest {
        private Long kbId;
        private List<Long> kbIds;
        private String query;
        private Integer topK;
    }

    @Data
    public static class RagSearchResponse {
        private final List<String> chunks;
    }
}
