package com.blog.file.service;

import com.blog.file.dto.ObjectMetaVO;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ObjectStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String defaultBucket;
    @Value("${minio.public-url:}")
    private String publicUrl;
    @Value("${minio.endpoint}")
    private String endpoint;

    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};

    public void ensureBucket(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("检查或创建桶失败: " + bucket, e);
        }
    }

    public ObjectMetaVO upload(MultipartFile file, String bucket, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("未上传文件或文件为空");
        }
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) originalFilename = "image";
        validateImage(contentType, originalFilename);

        String b = bucket != null && !bucket.isBlank() ? bucket : defaultBucket;
        ensureBucket(b);

        String key = buildKey(prefix, originalFilename);
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(b)
                            .object(key)
                            .stream(is, file.getSize(), -1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("上传失败: " + e.getMessage(), e);
        }

        long size = file.getSize();
        // 用路径形式 /api/objects/images/uuid.png，不编码斜杠，避免 %2F 被网关/服务器拒掉导致 400
        String url = "/api/objects/" + key + "?stream=1";
        return new ObjectMetaVO(key, url, size, contentType != null ? contentType : "application/octet-stream");
    }

    public ObjectMetaVO getMeta(String key, int expireSeconds) {
        String b = defaultBucket;
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(b).object(key).build());
        } catch (Exception e) {
            return null;
        }
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(b)
                            .object(key)
                            .expiry(expireSeconds, TimeUnit.SECONDS)
                            .build()
            );
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder().bucket(b).object(key).build());
            return new ObjectMetaVO(key, url, stat.size(), stat.contentType());
        } catch (Exception e) {
            String fallbackUrl = buildPublicUrl(b, key);
            try {
                StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder().bucket(b).object(key).build());
                return new ObjectMetaVO(key, fallbackUrl, stat.size(), stat.contentType());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public boolean delete(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(defaultBucket).object(key).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 流式获取对象，供浏览器直接展示（如图片）
     */
    public GetObjectResponse getObjectStream(String key) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder().bucket(defaultBucket).object(key).build()
        );
    }

    public String getObjectContentType(String key) {
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder().bucket(defaultBucket).object(key).build());
            return stat.contentType() != null ? stat.contentType() : "application/octet-stream";
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    private void validateImage(String contentType, String filename) {
        String lower = filename.toLowerCase();
        boolean extOk = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                extOk = true;
                break;
            }
        }
        if (!extOk) {
            throw new IllegalArgumentException("不支持的图片格式，仅允许 jpg、png、gif、webp");
        }
        if (contentType != null && !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("不支持的图片格式，仅允许 jpg、png、gif、webp");
        }
    }

    private String buildKey(String prefix, String originalFilename) {
        String ext = "png";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < originalFilename.length() - 1) {
            String e = originalFilename.substring(lastDot + 1).toLowerCase();
            if (e.matches("jpe?g|png|gif|webp")) ext = e.replace("jpeg", "jpg");
        }
        String safeName = java.util.UUID.randomUUID() + "." + ext;
        if (prefix != null && !prefix.isBlank()) {
            String p = prefix.startsWith("/") ? prefix.substring(1) : prefix;
            return p.endsWith("/") ? p + safeName : p + "/" + safeName;
        }
        return "images/" + safeName;
    }

    private String buildPublicUrl(String bucket, String key) {
        if (publicUrl != null && !publicUrl.isBlank()) {
            String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
            return base + "/" + bucket + "/" + key;
        }
        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return base + "/" + bucket + "/" + key;
    }
}
