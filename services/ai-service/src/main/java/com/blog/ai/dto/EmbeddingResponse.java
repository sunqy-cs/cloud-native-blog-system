package com.blog.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文本向量化响应：与请求中 texts 一一对应的向量列表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResponse {

    /**
     * 使用的模型名，如 text-embedding-v4。
     */
    private String model;

    /**
     * 向量列表，与请求 texts 顺序一致；每个元素为 float 数组。
     */
    private List<float[]> embeddings;
}
