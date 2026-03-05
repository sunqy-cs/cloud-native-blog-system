package com.blog.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * 文本向量化请求：支持单条或批量（最多 10 条）。
 */
@Data
public class EmbeddingRequest {

    /**
     * 待向量化的文本列表，与返回的 embeddings 顺序一致。
     */
    private List<String> texts;
}
