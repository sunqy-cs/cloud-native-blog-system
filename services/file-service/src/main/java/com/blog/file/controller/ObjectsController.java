package com.blog.file.controller;

import com.blog.file.dto.FromUrlRequest;
import com.blog.file.dto.ObjectMetaVO;
import com.blog.file.service.ObjectStorageService;
import io.minio.GetObjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/objects")
@RequiredArgsConstructor
public class ObjectsController {

    private final ObjectStorageService objectStorageService;

    /**
     * 创建对象（上传图片）
     * POST /api/objects
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObjectMetaVO> create(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bucket", required = false) String bucket,
            @RequestParam(value = "prefix", required = false) String prefix) {
        ObjectMetaVO meta = objectStorageService.upload(file, bucket, prefix);
        return ResponseEntity.status(HttpStatus.CREATED).body(meta);
    }

    /**
     * 从 URL 拉取图片并存入 MinIO（供 ai-service 等内部调用）
     * POST /api/objects/from-url
     */
    @PostMapping(value = "/from-url", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObjectMetaVO> createFromUrl(@RequestBody FromUrlRequest request) {
        String url = request != null ? request.getUrl() : null;
        String prefix = request != null ? request.getPrefix() : null;
        ObjectMetaVO meta = objectStorageService.uploadFromUrl(url, prefix);
        return ResponseEntity.status(HttpStatus.CREATED).body(meta);
    }

    /**
     * 获取对象：stream=1 时流式返回文件（供 img 等直接展示），否则返回元数据 JSON
     * GET /api/objects/{*key}
     */
    @GetMapping("/{*key}")
    public void get(
            @PathVariable("key") String key,
            @RequestParam(value = "stream", required = false) String stream,
            @RequestParam(value = "expire", defaultValue = "3600") int expireSeconds,
            HttpServletResponse response) throws Exception {
        String normalizedKey = key.startsWith("/") ? key.substring(1) : key;
        normalizedKey = URLDecoder.decode(normalizedKey, StandardCharsets.UTF_8);
        // 请求里可能带空格（如 Markdown 解析错误），MinIO 的 key 存的是下划线，统一规范化为下划线避免 400
        normalizedKey = normalizedKey.replace(" ", "_");
        if ("1".equals(stream)) {
            try {
                String contentType = objectStorageService.getObjectContentType(normalizedKey);
                response.setContentType(contentType);
                response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");
                try (GetObjectResponse obj = objectStorageService.getObjectStream(normalizedKey);
                     InputStream in = obj) {
                    in.transferTo(response.getOutputStream());
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            return;
        }
        ObjectMetaVO meta = objectStorageService.getMeta(normalizedKey, expireSeconds);
        if (meta == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getOutputStream().write(
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(meta));
    }

    /**
     * 删除对象
     * DELETE /api/objects/{*key}
     */
    @DeleteMapping("/{*key}")
    public ResponseEntity<Void> delete(@PathVariable("key") String key) {
        String normalizedKey = key.startsWith("/") ? key.substring(1) : key;
        normalizedKey = URLDecoder.decode(normalizedKey, StandardCharsets.UTF_8).replace(" ", "_");
        boolean removed = objectStorageService.delete(normalizedKey);
        if (!removed) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
