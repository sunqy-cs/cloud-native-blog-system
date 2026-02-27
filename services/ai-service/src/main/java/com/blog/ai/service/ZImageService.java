package com.blog.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 阿里云 DashScope 文生图（Z-Image），生成后上传至 file-service 并返回 MinIO URL。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZImageService {

    private static final String GENERATION_PATH = "/api/v1/services/aigc/multimodal-generation/generation";
    private static final String DEFAULT_SIZE = "1120*1440";

    private final RestTemplate restTemplate;

    @Value("${app.dashscope.base-url:https://dashscope.aliyuncs.com}")
    private String dashscopeBaseUrl;

    @Value("${app.dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${app.file-service.base-url:http://localhost:8083}")
    private String fileServiceBaseUrl;

    /**
     * 根据文本 prompt 生成图片：调用 DashScope → 将结果图拉取到 file-service 存入 MinIO → 返回本站 URL。
     */
    @SuppressWarnings("unchecked")
    public String generateImage(String prompt, String size) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt 不能为空");
        }
        if (dashscopeApiKey == null || dashscopeApiKey.isBlank()) {
            log.warn("DashScope API Key 未配置，请设置 DASHSCOPE_API_KEY");
            throw new IllegalStateException("DashScope API Key 未配置");
        }

        String url = (dashscopeBaseUrl == null || dashscopeBaseUrl.isBlank())
                ? "https://dashscope.aliyuncs.com"
                : dashscopeBaseUrl.replaceAll("/$", "");
        url = url + GENERATION_PATH;

        Map<String, Object> inputMessages = Map.of(
                "role", "user",
                "content", List.of(Map.of("text", prompt.trim()))
        );
        Map<String, Object> input = Map.of("messages", List.of(inputMessages));
        Map<String, Object> parameters = Map.of(
                "prompt_extend", false,
                "size", (size != null && !size.isBlank()) ? size.trim() : DEFAULT_SIZE
        );
        Map<String, Object> body = Map.of(
                "model", "z-image-turbo",
                "input", input,
                "parameters", parameters
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(dashscopeApiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
        if (response == null) {
            throw new RuntimeException("DashScope 文生图返回为空");
        }

        String imageUrl = extractImageUrl(response);
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new RuntimeException("DashScope 返回中未包含图片 URL");
        }

        String fileServiceUrl = (fileServiceBaseUrl == null || fileServiceBaseUrl.isBlank())
                ? "http://localhost:8083"
                : fileServiceBaseUrl.replaceAll("/$", "");
        String fromUrlEndpoint = fileServiceUrl + "/api/objects/from-url";
        Map<String, String> fromUrlBody = Map.of(
                "url", imageUrl,
                "prefix", "ai"
        );
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> fromUrlEntity = new HttpEntity<>(fromUrlBody, fileHeaders);
        Map<String, Object> meta = restTemplate.postForObject(fromUrlEndpoint, fromUrlEntity, Map.class);
        if (meta == null || !meta.containsKey("url")) {
            throw new RuntimeException("file-service 保存图片失败");
        }
        Object urlObj = meta.get("url");
        return urlObj != null ? urlObj.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private String extractImageUrl(Map<String, Object> response) {
        Object output = response.get("output");
        if (!(output instanceof Map)) return null;
        Object choices = ((Map<String, Object>) output).get("choices");
        if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) return null;
        Object first = ((List<?>) choices).get(0);
        if (!(first instanceof Map)) return null;
        Object message = ((Map<String, Object>) first).get("message");
        if (!(message instanceof Map)) return null;
        Object content = ((Map<String, Object>) message).get("content");
        if (!(content instanceof List)) return null;
        for (Object item : (List<?>) content) {
            if (!(item instanceof Map)) continue;
            Object image = ((Map<String, Object>) item).get("image");
            if (image != null && image.toString().startsWith("http")) {
                return image.toString();
            }
        }
        return null;
    }
}
