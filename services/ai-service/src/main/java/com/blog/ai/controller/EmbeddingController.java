package com.blog.ai.controller;

import com.blog.ai.dto.EmbeddingRequest;
import com.blog.ai.dto.EmbeddingResponse;
import com.blog.ai.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 阿里云百炼文本向量化（Embedding）接口，供 RAG、检索等使用。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    /**
     * 文本向量化。请求体为 {"texts": ["文本1", "文本2"]}，单次最多 10 条。
     */
    @PostMapping("/embeddings")
    public ResponseEntity<EmbeddingResponse> embeddings(@RequestBody EmbeddingRequest request) {
        if (request == null || request.getTexts() == null || request.getTexts().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<float[]> embeddings = embeddingService.embed(request.getTexts());
        EmbeddingResponse response = new EmbeddingResponse("text-embedding-v4", embeddings);
        return ResponseEntity.ok(response);
    }
}
