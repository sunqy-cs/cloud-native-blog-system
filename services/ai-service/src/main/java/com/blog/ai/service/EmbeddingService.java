package com.blog.ai.service;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 阿里云百炼文本向量化（Embedding），用于 RAG 检索等。使用 text-embedding-v4。
 */
@Slf4j
@Service
public class EmbeddingService {

    private static final String DEFAULT_MODEL = "text-embedding-v4";
    private static final int DEFAULT_DIMENSION = 1024;

    @Value("${app.dashscope.embedding-model:" + DEFAULT_MODEL + "}")
    private String model;

    @Value("${app.dashscope.embedding-dimension:" + DEFAULT_DIMENSION + "}")
    private int dimension;

    /**
     * 单条文本向量化。
     */
    public float[] embed(String text) {
        List<float[]> list = embed(Collections.singletonList(text));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 批量文本向量化，与输入顺序一致。单次最多 10 条（百炼 text-embedding-v4 限制）。
     */
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        if (texts.size() > 10) {
            log.warn("单次请求超过 10 条，将只处理前 10 条");
            texts = texts.subList(0, 10);
        }
        List<String> valid = texts.stream()
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
        if (valid.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .model(model != null && !model.isBlank() ? model : DEFAULT_MODEL)
                    .texts(valid)
                    .dimension(dimension > 0 ? dimension : DEFAULT_DIMENSION)
                    .build();
            TextEmbedding textEmbedding = new TextEmbedding();
            TextEmbeddingResult result = textEmbedding.call(param);

            if (result == null || result.getOutput() == null || result.getOutput().getEmbeddings() == null) {
                log.warn("Embedding 返回为空");
                return Collections.emptyList();
            }
            return result.getOutput().getEmbeddings().stream()
                    .map(e -> e.getEmbedding() != null ? toFloatArray(e.getEmbedding()) : null)
                    .collect(Collectors.toList());
        } catch (NoApiKeyException e) {
            log.error("DashScope API Key 未配置: {}", e.getMessage());
            throw new IllegalStateException("DashScope API Key 未配置，无法调用 Embedding", e);
        } catch (ApiException e) {
            log.error("调用百炼 Embedding 失败: {}", e.getMessage());
            throw new RuntimeException("Embedding 调用失败: " + e.getMessage(), e);
        }
    }

    private static float[] toFloatArray(List<Double> list) {
        if (list == null) return null;
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Double d = list.get(i);
            arr[i] = d != null ? d.floatValue() : 0f;
        }
        return arr;
    }
}
