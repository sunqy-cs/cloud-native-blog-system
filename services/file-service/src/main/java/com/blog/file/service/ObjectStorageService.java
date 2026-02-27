package com.blog.file.service;

import com.blog.file.dto.ObjectMetaVO;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
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

    private static final String ALLOWED_FROM_URL_HOST = "dashscope-result-bj.oss-cn-beijing.aliyuncs.com";

    /**
     * 从允许的 URL（当前仅 DashScope 结果 OSS）下载图片并上传到 MinIO，返回本站访问 URL。
     */
    public ObjectMetaVO uploadFromUrl(String imageUrl, String prefix) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("url 不能为空");
        }
        try {
            URI uri = URI.create(imageUrl.trim());
            String host = uri.getHost();
            if (host == null || !host.equals(ALLOWED_FROM_URL_HOST)) {
                throw new IllegalArgumentException("仅支持从阿里云 DashScope 结果域名拉取，当前 host: " + host);
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
            throw new IllegalArgumentException("无效的 url: " + e.getMessage());
        }

        String p = (prefix != null && !prefix.isBlank()) ? prefix : "ai";
        String key = buildKey(p, "ai-gen.png");
        String bucket = defaultBucket;
        ensureBucket(bucket);

        String contentType = "image/png";
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(imageUrl.trim()).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(60_000);
            conn.connect();
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new RuntimeException("下载图片失败: HTTP " + code);
            }
            String ct = conn.getContentType();
            if (ct != null && ct.toLowerCase().startsWith("image/")) {
                contentType = ct;
            }
            try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                byte[] bytes = out.toByteArray();
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(key)
                                .stream(new java.io.ByteArrayInputStream(bytes), bytes.length, -1)
                                .contentType(contentType)
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("从 URL 拉取并上传失败: " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }

        String url = "/api/objects/" + key + "?stream=1";
        try {
            long size = minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build()).size();
            return new ObjectMetaVO(key, url, size, contentType);
        } catch (Exception e) {
            return new ObjectMetaVO(key, url, 0, contentType);
        }
    }
}
