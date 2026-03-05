package com.blog.content.client;

import lombok.Data;

import java.util.List;

/** 与 ai-service POST /api/ai/embeddings 请求/响应结构一致。 */
public class EmbeddingClient {

    @Data
    public static class Request {
        private List<String> texts;
    }

    @Data
    public static class Response {
        private String model;
        private List<List<Double>> embeddings;
    }
}
