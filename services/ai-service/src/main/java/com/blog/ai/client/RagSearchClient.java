package com.blog.ai.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
public class RagSearchClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RagSearchClient(RestTemplate restTemplate,
                          @org.springframework.beans.factory.annotation.Value("${app.content-service-url:http://localhost:8084}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.replaceAll("/$", "");
    }

    public List<String> search(Long kbId, String query, int topK) {
        Request req = new Request();
        req.setKbId(kbId);
        req.setQuery(query);
        req.setTopK(topK);
        return doSearch(req);
    }

    /** 多知识库检索（或空列表表示不检索） */
    public List<String> searchWithKbIds(List<Long> kbIds, String query, int topK) {
        if (kbIds == null || kbIds.isEmpty()) return Collections.emptyList();
        Request req = new Request();
        req.setKbIds(kbIds);
        req.setQuery(query);
        req.setTopK(topK);
        return doSearch(req);
    }

    private List<String> doSearch(Request req) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Request> entity = new HttpEntity<>(req, headers);
        try {
            Response res = restTemplate.postForObject(baseUrl + "/internal/kb/rag-search", entity, Response.class);
            return res != null && res.getChunks() != null ? res.getChunks() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Data
    public static class Request {
        private Long kbId;
        private List<Long> kbIds;
        private String query;
        private Integer topK;
    }

    @Data
    public static class Response {
        private List<String> chunks;
    }
}
