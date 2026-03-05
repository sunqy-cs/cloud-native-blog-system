package com.blog.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.client.EmbeddingClient;
import com.blog.content.entity.Content;
import com.blog.content.entity.KnowledgeBaseContent;
import com.blog.content.mapper.ContentMapper;
import com.blog.content.mapper.KnowledgeBaseContentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库向量索引：切块、调 embedding、写入/删除 pgvector。供 addContent/removeContent/delete 与内容更新、发布时调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbVectorService {

    private static final int CHUNK_MAX_SIZE = 800;
    private static final int CHUNK_SPLIT_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private static final int EMBEDDING_BATCH_SIZE = 10;

    @Qualifier("vectorJdbcTemplate")
    private final JdbcTemplate vectorJdbcTemplate;
    private final RestTemplate restTemplate;
    private final ContentMapper contentMapper;
    private final KnowledgeBaseContentMapper knowledgeBaseContentMapper;

    @Value("${app.ai-service-url:http://localhost:8086}")
    private String aiServiceUrl;

    /**
     * 为知识库下某篇内容建立向量索引：先删旧再切块、向量化、写入。
     */
    public void indexContentForKb(Long kbId, Long contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null || content.getBody() == null) {
            return;
        }
        String body = content.getBody().trim();
        if (body.isEmpty()) {
            deleteByKbAndContent(kbId, contentId);
            return;
        }
        deleteByKbAndContent(kbId, contentId);
        List<String> chunks = chunk(body);
        if (chunks.isEmpty()) return;
        List<float[]> allEmbeddings = callEmbeddingBatched(chunks);
        if (allEmbeddings.size() != chunks.size()) {
            log.warn("Embedding 条数与 chunks 不一致，跳过写入 pgvector contentId={}", contentId);
            return;
        }
        insertChunks(kbId, contentId, chunks, allEmbeddings);
        log.info("已为知识库内容建向量索引 kbId={} contentId={} chunks={}", kbId, contentId, chunks.size());
    }

    /**
     * 删除该知识库下该内容的所有向量。
     */
    public void deleteByKbAndContent(Long kbId, Long contentId) {
        int n = vectorJdbcTemplate.update(
                "DELETE FROM kb_chunk_embedding WHERE kb_id = ? AND content_id = ?",
                kbId, contentId);
        if (n > 0) log.debug("已删除向量 kbId={} contentId={} rows={}", kbId, contentId, n);
    }

    /**
     * 删除该知识库下所有向量。
     */
    public void deleteByKbId(Long kbId) {
        int n = vectorJdbcTemplate.update("DELETE FROM kb_chunk_embedding WHERE kb_id = ?", kbId);
        if (n > 0) log.info("已删除知识库下全部向量 kbId={} rows={}", kbId, n);
    }

    /**
     * RAG 检索：根据问题文本在知识库中做向量相似检索，返回最相关的若干片段（仅文本）。
     * 供 ai-service RAG 问答调用。
     */
    public List<String> searchChunks(Long kbId, String query, int topK) {
        if (kbId == null || query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        return searchChunksMultipleKbs(List.of(kbId), query, topK);
    }

    /**
     * 多知识库 RAG 检索：在多个知识库中分别检索后合并，按相关度顺序返回 topK 条。
     */
    public List<String> searchChunksMultipleKbs(List<Long> kbIds, String query, int topK) {
        if (kbIds == null || kbIds.isEmpty() || query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        List<float[]> vecs = callEmbeddingBatched(List.of(query.trim()));
        if (vecs.isEmpty()) return List.of();
        String vecStr = toVectorString(vecs.get(0));
        int perKb = Math.max(1, (topK + kbIds.size() - 1) / kbIds.size());
        List<String> merged = new ArrayList<>();
        for (Long kbId : kbIds) {
            String sql = "SELECT content FROM kb_chunk_embedding WHERE kb_id = ? ORDER BY embedding <=> ?::vector LIMIT ?";
            List<String> part = vectorJdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("content"), kbId, vecStr, perKb);
            merged.addAll(part);
        }
        return merged.size() <= topK ? merged : merged.subList(0, topK);
    }

    /**
     * 当某篇内容被更新或发布时：若该内容属于若干知识库，则刷新这些知识库下该内容的向量。
     */
    public void refreshEmbeddingsForContent(Long contentId) {
        List<KnowledgeBaseContent> list = knowledgeBaseContentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseContent>()
                        .eq(KnowledgeBaseContent::getContentId, contentId));
        if (list.isEmpty()) return;
        for (KnowledgeBaseContent kbc : list) {
            try {
                indexContentForKb(kbc.getKnowledgeBaseId(), contentId);
            } catch (Exception e) {
                log.warn("刷新知识库向量失败 kbId={} contentId={}", kbc.getKnowledgeBaseId(), contentId, e);
            }
        }
    }

    private List<String> chunk(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> segments = new ArrayList<>();
        for (String para : text.split("\\n\\n+")) {
            String p = para.trim();
            if (p.isEmpty()) continue;
            if (p.length() <= CHUNK_MAX_SIZE) {
                segments.add(p);
            } else {
                for (int i = 0; i < p.length(); i += (CHUNK_SPLIT_SIZE - CHUNK_OVERLAP)) {
                    int end = Math.min(i + CHUNK_SPLIT_SIZE, p.length());
                    segments.add(p.substring(i, end));
                    if (end >= p.length()) break;
                }
            }
        }
        return segments;
    }

    private List<float[]> callEmbeddingBatched(List<String> texts) {
        List<float[]> result = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += EMBEDDING_BATCH_SIZE) {
            List<String> batch = texts.subList(i, Math.min(i + EMBEDDING_BATCH_SIZE, texts.size()));
            EmbeddingClient.Request req = new EmbeddingClient.Request();
            req.setTexts(batch);
            String url = aiServiceUrl.replaceFirst("/$", "") + "/api/ai/embeddings";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<EmbeddingClient.Request> entity = new HttpEntity<>(req, headers);
            try {
                EmbeddingClient.Response resp = restTemplate.postForObject(url, entity, EmbeddingClient.Response.class);
                if (resp != null && resp.getEmbeddings() != null) {
                    for (List<Double> emb : resp.getEmbeddings()) {
                        result.add(toFloatArray(emb));
                    }
                }
            } catch (Exception e) {
                log.error("调用 embedding 失败 batchSize={}", batch.size(), e);
                throw new RuntimeException("Embedding 调用失败", e);
            }
        }
        return result;
    }

    private static float[] toFloatArray(List<Double> list) {
        if (list == null) return new float[0];
        float[] a = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Double d = list.get(i);
            a[i] = d != null ? d.floatValue() : 0f;
        }
        return a;
    }

    private void insertChunks(Long kbId, Long contentId, List<String> chunks, List<float[]> embeddings) {
        String sql = "INSERT INTO kb_chunk_embedding (kb_id, content_id, chunk_index, content, embedding) VALUES (?, ?, ?, ?, ?::vector)";
        for (int i = 0; i < chunks.size(); i++) {
            String vecStr = toVectorString(embeddings.get(i));
            vectorJdbcTemplate.update(sql, kbId, contentId, i, chunks.get(i), vecStr);
        }
    }

    private static String toVectorString(float[] v) {
        if (v == null || v.length == 0) return "[]";
        StringBuilder sb = new StringBuilder().append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
