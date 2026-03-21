package com.blog.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.content.dto.ModerationHumanReviewRequest;
import com.blog.content.dto.ModerationStatsResponse;
import com.blog.content.dto.ModerationSubmitRequest;
import com.blog.content.dto.ModerationTaskPageResponse;
import com.blog.content.dto.ModerationTaskVO;
import com.blog.content.entity.Content;
import com.blog.content.entity.Column;
import com.blog.content.entity.KnowledgeBase;
import com.blog.content.entity.KnowledgeBaseContent;
import com.blog.content.entity.ModerationComment;
import com.blog.content.entity.ModerationTask;
import com.blog.content.entity.ModerationUser;
import com.blog.content.entity.UserMessage;
import com.blog.content.mapper.ContentMapper;
import com.blog.content.mapper.ColumnMapper;
import com.blog.content.mapper.KnowledgeBaseContentMapper;
import com.blog.content.mapper.KnowledgeBaseMapper;
import com.blog.content.mapper.ModerationCommentMapper;
import com.blog.content.mapper.ModerationTaskMapper;
import com.blog.content.mapper.ModerationUserMapper;
import com.blog.content.mapper.UserMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModerationService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_NEEDS_HUMAN = "NEEDS_HUMAN";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private static final String RESOURCE_ARTICLE = "ARTICLE";
    private static final String RESOURCE_KNOWLEDGE_DOC = "KNOWLEDGE_DOC";
    private static final String RESOURCE_COMMENT = "COMMENT";
    private static final String RESOURCE_USER_PROFILE = "USER_PROFILE";
    private static final String RESOURCE_COLUMN = "COLUMN";
    private static final String RESOURCE_KNOWLEDGE_BASE = "KNOWLEDGE_BASE";
    private static final String KB_VISIBILITY_PRIVATE = "PRIVATE";

    private final ModerationTaskMapper moderationTaskMapper;
    private final ModerationUserMapper moderationUserMapper;
    private final ContentMapper contentMapper;
    private final ColumnMapper columnMapper;
    private final ModerationCommentMapper moderationCommentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseContentMapper knowledgeBaseContentMapper;
    private final UserMessageMapper userMessageMapper;
    private final RestTemplate restTemplate;

    @Value("${app.ai-service-url:http://localhost:8086}")
    private String aiServiceUrl;

    public boolean isAdmin(Long userId) {
        if (userId == null) return false;
        ModerationUser u = moderationUserMapper.selectById(userId);
        return u != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    private void requireAdmin(Long userId) {
        if (!isAdmin(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可操作");
        }
    }

    @Transactional
    public ModerationTask submitTask(ModerationSubmitRequest req) {
        if (req == null || req.getResourceId() == null || req.getOwnerUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少必要参数");
        }
        String resourceType = normalizeResourceType(req.getResourceType());
        // 管理员内容免审：返回空任务态
        if (isAdmin(req.getOwnerUserId())) {
            return null;
        }

        ModerationTask task = moderationTaskMapper.selectOne(
                new LambdaQueryWrapper<ModerationTask>()
                        .eq(ModerationTask::getResourceType, resourceType)
                        .eq(ModerationTask::getResourceId, req.getResourceId())
                        .last("LIMIT 1"));

        LocalDateTime now = LocalDateTime.now();
        String payloadJson = normalizePayloadSnapshot(req.getPayloadSnapshot());
        if (task == null) {
            task = new ModerationTask();
            task.setResourceType(resourceType);
            task.setResourceId(req.getResourceId());
            task.setOwnerUserId(req.getOwnerUserId());
            task.setStatus(STATUS_PENDING);
            task.setPayloadSnapshot(payloadJson);
            moderationTaskMapper.insert(task);
        } else {
            task.setOwnerUserId(req.getOwnerUserId());
            task.setStatus(STATUS_PENDING);
            task.setAiDecision(null);
            task.setAiDetail(null);
            task.setAiReviewedAt(null);
            task.setHumanDecision(null);
            task.setHumanReviewerId(null);
            task.setHumanReviewedAt(null);
            task.setHumanNote(null);
            task.setPayloadSnapshot(payloadJson);
            task.setUpdatedAt(now);
            moderationTaskMapper.updateById(task);
        }

        Map<String, Object> ai = callAiModeration(resourceType, req.getPayloadSnapshot());
        String decision = aiDecision(ai);
        boolean requireSecondHuman = shouldRequireHumanSecondReview(resourceType, req.getPayloadSnapshot());
        if (requireSecondHuman && !"REJECT".equalsIgnoreCase(decision)) {
            ai = overrideAiReason(ai, "AI初审通过，包含图片内容，需人工二次审核");
        }
        task.setAiDecision(decision);
        task.setAiDetail(ai != null ? safeJson(ai) : null);
        task.setAiReviewedAt(now);

        if ("PASS".equalsIgnoreCase(decision)) {
            if (requireSecondHuman) {
                task.setStatus(STATUS_NEEDS_HUMAN);
                task.setHumanDecision(null);
                task.setHumanReviewerId(null);
                task.setHumanReviewedAt(null);
                markResourceModerationStatus(task, STATUS_NEEDS_HUMAN);
            } else {
                task.setStatus(STATUS_APPROVED);
                task.setHumanDecision("APPROVE");
                task.setHumanReviewedAt(now);
                applyFinalDecision(task, STATUS_APPROVED);
            }
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            task.setStatus(STATUS_REJECTED);
            task.setHumanDecision("REJECT");
            task.setHumanReviewedAt(now);
            applyFinalDecision(task, STATUS_REJECTED);
        } else {
            task.setStatus(STATUS_NEEDS_HUMAN);
            markResourceModerationStatus(task, STATUS_NEEDS_HUMAN);
        }

        moderationTaskMapper.updateById(task);
        return task;
    }

    /**
     * payload_snapshot 列为 JSON：允许传入纯文本或 JSON 字符串，最终都转为合法 JSON 文本。
     * - 传入对象/数组 JSON：原样保存
     * - 传入普通文本：包装成 {"text":"..."}
     */
    private String normalizePayloadSnapshot(String raw) {
        String s = raw == null ? "" : raw.trim();
        try {
            if (!s.isEmpty()) {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var node = mapper.readTree(s);
                if (node != null && (node.isObject() || node.isArray())) {
                    return s;
                }
            }
        } catch (Exception ignored) {
            // 非 JSON，按文本包装
        }
        return "{\"text\":" + quoteJson(s) + "}";
    }

    private String quoteJson(String text) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(text == null ? "" : text);
        } catch (Exception e) {
            return "\"\"";
        }
    }

    public ModerationTaskPageResponse listTasks(
            Long adminUserId, int page, int pageSize, String resourceType, String status, boolean finishedOnly) {
        requireAdmin(adminUserId);
        Page<ModerationTask> pg = new Page<>(Math.max(page, 1), pageSize <= 0 ? 20 : Math.min(pageSize, 100));
        LambdaQueryWrapper<ModerationTask> q = new LambdaQueryWrapper<>();
        if (resourceType != null && !resourceType.isBlank()) q.eq(ModerationTask::getResourceType, normalizeResourceType(resourceType));
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) q.eq(ModerationTask::getStatus, status.trim().toUpperCase());
        if (finishedOnly) {
            q.in(ModerationTask::getStatus, List.of(STATUS_APPROVED, STATUS_REJECTED));
        }
        q.orderByDesc(ModerationTask::getUpdatedAt).orderByDesc(ModerationTask::getId);
        Page<ModerationTask> result = moderationTaskMapper.selectPage(pg, q);
        ModerationTaskPageResponse res = new ModerationTaskPageResponse();
        res.setTotal(result.getTotal());
        res.setRecords(result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return res;
    }

    public ModerationStatsResponse stats(Long adminUserId) {
        requireAdmin(adminUserId);
        long pending = moderationTaskMapper.selectCount(new LambdaQueryWrapper<ModerationTask>()
                .eq(ModerationTask::getStatus, STATUS_PENDING));
        long pendingHuman = moderationTaskMapper.selectCount(new LambdaQueryWrapper<ModerationTask>()
                .eq(ModerationTask::getStatus, STATUS_NEEDS_HUMAN));
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(7).atStartOfDay();
        long todayFinished = moderationTaskMapper.selectCount(new LambdaQueryWrapper<ModerationTask>()
                .in(ModerationTask::getStatus, List.of(STATUS_APPROVED, STATUS_REJECTED))
                .ge(ModerationTask::getUpdatedAt, todayStart));
        long rejected7d = moderationTaskMapper.selectCount(new LambdaQueryWrapper<ModerationTask>()
                .eq(ModerationTask::getStatus, STATUS_REJECTED)
                .ge(ModerationTask::getUpdatedAt, sevenDaysAgo));
        ModerationStatsResponse res = new ModerationStatsResponse();
        res.setPending(pending);
        res.setPendingHuman(pendingHuman);
        res.setTodayFinished(todayFinished);
        res.setRejected7d(rejected7d);
        return res;
    }

    @Transactional
    public ModerationTaskVO humanReview(Long adminUserId, Long taskId, ModerationHumanReviewRequest req) {
        requireAdmin(adminUserId);
        ModerationTask task = moderationTaskMapper.selectById(taskId);
        if (task == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "审核任务不存在");
        String decision = req != null && req.getDecision() != null ? req.getDecision().trim().toUpperCase() : "";
        if (!"APPROVE".equals(decision) && !"REJECT".equals(decision)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "decision 仅支持 APPROVE/REJECT");
        }
        String finalStatus = "APPROVE".equals(decision) ? STATUS_APPROVED : STATUS_REJECTED;
        task.setStatus(finalStatus);
        task.setHumanDecision(decision);
        task.setHumanReviewerId(adminUserId);
        task.setHumanReviewedAt(LocalDateTime.now());
        task.setHumanNote(req != null ? req.getNote() : null);
        moderationTaskMapper.updateById(task);
        applyFinalDecision(task, finalStatus);
        return toVO(moderationTaskMapper.selectById(taskId));
    }

    @Transactional
    public ModerationTaskVO rerunAiReview(Long adminUserId, Long taskId) {
        requireAdmin(adminUserId);
        ModerationTask task = moderationTaskMapper.selectById(taskId);
        if (task == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "审核任务不存在");
        Map<String, Object> ai = callAiModeration(task.getResourceType(), task.getPayloadSnapshot());
        String decision = aiDecision(ai);
        boolean requireSecondHuman = shouldRequireHumanSecondReview(task.getResourceType(), task.getPayloadSnapshot());
        if (requireSecondHuman && !"REJECT".equalsIgnoreCase(decision)) {
            ai = overrideAiReason(ai, "AI初审通过，包含图片内容，需人工二次审核");
        }
        task.setAiDecision(decision);
        task.setAiDetail(ai != null ? safeJson(ai) : null);
        task.setAiReviewedAt(LocalDateTime.now());
        if ("PASS".equalsIgnoreCase(decision)) {
            if (requireSecondHuman) {
                task.setStatus(STATUS_NEEDS_HUMAN);
                task.setHumanDecision(null);
                task.setHumanReviewerId(null);
                task.setHumanReviewedAt(null);
                markResourceModerationStatus(task, STATUS_NEEDS_HUMAN);
            } else {
                task.setStatus(STATUS_APPROVED);
                task.setHumanDecision("APPROVE");
                task.setHumanReviewerId(null);
                task.setHumanReviewedAt(LocalDateTime.now());
                applyFinalDecision(task, STATUS_APPROVED);
            }
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            task.setStatus(STATUS_REJECTED);
            task.setHumanDecision("REJECT");
            task.setHumanReviewedAt(LocalDateTime.now());
            applyFinalDecision(task, STATUS_REJECTED);
        } else {
            task.setStatus(STATUS_NEEDS_HUMAN);
            task.setHumanDecision(null);
            task.setHumanReviewerId(null);
            task.setHumanReviewedAt(null);
            markResourceModerationStatus(task, STATUS_NEEDS_HUMAN);
        }
        moderationTaskMapper.updateById(task);
        return toVO(task);
    }

    private void applyFinalDecision(ModerationTask task, String finalStatus) {
        markResourceModerationStatus(task, finalStatus);
        sendResultMessage(task, finalStatus);
    }

    private void markResourceModerationStatus(ModerationTask task, String moderationStatus) {
        String rt = task.getResourceType();
        if (RESOURCE_ARTICLE.equals(rt) || RESOURCE_KNOWLEDGE_DOC.equals(rt)) {
            Content c = contentMapper.selectById(task.getResourceId());
            if (c != null) {
                c.setModerationStatus(moderationStatus);
                // 对博客：审核通过时自动发布；驳回或待人工保持草稿
                if (RESOURCE_ARTICLE.equals(rt) && STATUS_APPROVED.equals(moderationStatus)) {
                    c.setStatus("PUBLISHED");
                }
                if (RESOURCE_ARTICLE.equals(rt) && STATUS_REJECTED.equals(moderationStatus)) {
                    c.setStatus("DRAFT");
                }
                contentMapper.updateById(c);
                // 知识库文件未通过：将所在知识库设为私有
                if (RESOURCE_KNOWLEDGE_DOC.equals(rt) && STATUS_REJECTED.equals(moderationStatus)) {
                    setKnowledgeBasesPrivateByContentId(task.getResourceId());
                }
            }
            return;
        }
        if (RESOURCE_COMMENT.equals(rt)) {
            ModerationComment comment = moderationCommentMapper.selectById(task.getResourceId());
            if (comment != null) {
                if (STATUS_REJECTED.equals(moderationStatus)) {
                    moderationCommentMapper.deleteById(comment.getId());
                } else {
                    comment.setModerationStatus(moderationStatus);
                    moderationCommentMapper.updateById(comment);
                }
            }
            return;
        }
        if (RESOURCE_USER_PROFILE.equals(rt)) {
            ModerationUser user = moderationUserMapper.selectById(task.getResourceId());
            if (user != null) {
                user.setProfileModerationStatus(moderationStatus);
                if (STATUS_REJECTED.equals(moderationStatus)) {
                    // 资料未通过时隐藏资料字段，并给出审核说明
                    user.setNickname(null);
                    user.setAvatar(null);
                    user.setCover(null);
                    user.setGender(null);
                    user.setIntro(null);
                    user.setResidence(null);
                    user.setIndustry(null);
                    user.setBio("该用户个人信息未通过审核，资料已隐藏");
                }
                moderationUserMapper.updateById(user);
            }
            return;
        }
        if (RESOURCE_KNOWLEDGE_BASE.equals(rt)) {
            if (STATUS_REJECTED.equals(moderationStatus)) {
                KnowledgeBase kb = knowledgeBaseMapper.selectById(task.getResourceId());
                if (kb != null) {
                    kb.setVisibility(KB_VISIBILITY_PRIVATE);
                    knowledgeBaseMapper.updateById(kb);
                }
            }
            return;
        }
        if (RESOURCE_COLUMN.equals(rt)) {
            if (STATUS_REJECTED.equals(moderationStatus)) {
                columnMapper.deleteById(task.getResourceId());
            }
            return;
        }
    }

    private void setKnowledgeBasesPrivateByContentId(Long contentId) {
        if (contentId == null) return;
        List<KnowledgeBaseContent> rel = knowledgeBaseContentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseContent>()
                        .eq(KnowledgeBaseContent::getContentId, contentId));
        if (rel == null || rel.isEmpty()) return;
        for (KnowledgeBaseContent r : rel) {
            if (r == null || r.getKnowledgeBaseId() == null) continue;
            KnowledgeBase kb = knowledgeBaseMapper.selectById(r.getKnowledgeBaseId());
            if (kb == null) continue;
            if (!KB_VISIBILITY_PRIVATE.equalsIgnoreCase(kb.getVisibility())) {
                kb.setVisibility(KB_VISIBILITY_PRIVATE);
                knowledgeBaseMapper.updateById(kb);
            }
        }
    }

    private void sendResultMessage(ModerationTask task, String finalStatus) {
        UserMessage msg = new UserMessage();
        msg.setUserId(task.getOwnerUserId());
        msg.setSenderUserId(null);
        msg.setMsgType("AUDIT");
        msg.setScene("MODERATION_RESULT");
        String typeText = switch (task.getResourceType()) {
            case RESOURCE_ARTICLE -> "博客";
            case RESOURCE_KNOWLEDGE_DOC -> "知识库文档";
            case RESOURCE_COMMENT -> "评论";
            case RESOURCE_USER_PROFILE -> "个人资料";
            case RESOURCE_COLUMN -> "专栏";
            case RESOURCE_KNOWLEDGE_BASE -> "知识库";
            default -> "内容";
        };
        String targetName = resolveTargetName(task, typeText);
        if (STATUS_APPROVED.equals(finalStatus)) {
            msg.setTitle(typeText + "审核通过");
            msg.setBody("你的" + typeText + "《" + targetName + "》已通过审核。");
        } else if (STATUS_REJECTED.equals(finalStatus)) {
            msg.setTitle(typeText + "审核未通过");
            String reason = resolveRejectReason(task);
            msg.setBody("你的" + typeText + "《" + targetName + "》未通过审核。原因：" + reason + "。请修改后重新提交。");
        } else {
            msg.setTitle(typeText + "待人工审核");
            msg.setBody("你的" + typeText + "《" + targetName + "》正在等待人工审核。");
        }
        msg.setExtra("{\"resourceType\":\"" + task.getResourceType() + "\",\"resourceId\":" + task.getResourceId() + "}");
        userMessageMapper.insert(msg);
    }

    private String resolveTargetName(ModerationTask task, String typeText) {
        if (RESOURCE_ARTICLE.equals(task.getResourceType()) || RESOURCE_KNOWLEDGE_DOC.equals(task.getResourceType())) {
            Content c = contentMapper.selectById(task.getResourceId());
            if (c != null && c.getTitle() != null && !c.getTitle().isBlank()) {
                return c.getTitle().trim();
            }
            String fromPayload = extractTitleFromPayload(task.getPayloadSnapshot());
            if (fromPayload != null && !fromPayload.isBlank()) return fromPayload;
        }
        if (RESOURCE_COMMENT.equals(task.getResourceType())) {
            ModerationComment c = moderationCommentMapper.selectById(task.getResourceId());
            if (c != null && c.getBody() != null && !c.getBody().isBlank()) {
                return shorten(c.getBody(), 24);
            }
            String fromPayload = extractBodyPreviewFromPayload(task.getPayloadSnapshot());
            if (fromPayload != null && !fromPayload.isBlank()) return shorten(fromPayload, 24);
        }
        if (RESOURCE_USER_PROFILE.equals(task.getResourceType())) {
            ModerationUser u = moderationUserMapper.selectById(task.getResourceId());
            if (u != null) {
                if (u.getNickname() != null && !u.getNickname().isBlank()) return u.getNickname().trim();
                if (u.getUsername() != null && !u.getUsername().isBlank()) return u.getUsername().trim();
            }
            String fromPayload = extractNicknameFromPayload(task.getPayloadSnapshot());
            if (fromPayload != null && !fromPayload.isBlank()) return fromPayload;
        }
        if (RESOURCE_COLUMN.equals(task.getResourceType())) {
            Column col = columnMapper.selectById(task.getResourceId());
            if (col != null && col.getName() != null && !col.getName().isBlank()) return col.getName().trim();
            String fromPayload = extractTitleFromPayload(task.getPayloadSnapshot());
            if (fromPayload != null && !fromPayload.isBlank()) return fromPayload;
        }
        if (RESOURCE_KNOWLEDGE_BASE.equals(task.getResourceType())) {
            KnowledgeBase kb = knowledgeBaseMapper.selectById(task.getResourceId());
            if (kb != null && kb.getName() != null && !kb.getName().isBlank()) return kb.getName().trim();
            String fromPayload = extractTitleFromPayload(task.getPayloadSnapshot());
            if (fromPayload != null && !fromPayload.isBlank()) return fromPayload;
        }
        return typeText;
    }

    private String resolveRejectReason(ModerationTask task) {
        if (task.getHumanNote() != null && !task.getHumanNote().isBlank()) {
            return task.getHumanNote().trim();
        }
        String aiDetail = task.getAiDetail();
        if (aiDetail != null && !aiDetail.isBlank()) {
            try {
                var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(aiDetail);
                if (node != null && node.has("reason")) {
                    String r = node.path("reason").asText("").trim();
                    if (!r.isBlank()) return r;
                }
            } catch (Exception ignored) {
                // ignore parse failure and fallback below
            }
        }
        return "触发平台审核规则";
    }

    private String normalizeResourceType(String v) {
        String t = v == null ? "" : v.trim().toUpperCase();
        if (RESOURCE_ARTICLE.equals(t) || RESOURCE_KNOWLEDGE_DOC.equals(t) || RESOURCE_COMMENT.equals(t)
                || RESOURCE_USER_PROFILE.equals(t) || RESOURCE_COLUMN.equals(t) || RESOURCE_KNOWLEDGE_BASE.equals(t)) {
            return t;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resourceType 不合法");
    }

    private Map<String, Object> callAiModeration(String resourceType, String payload) {
        String url = aiServiceUrl.replaceFirst("/$", "") + "/api/ai/moderation/review";
        Map<String, Object> body = Map.of(
                "resourceType", resourceType,
                "content", payload == null ? "" : payload
        );
        try {
            return restTemplate.postForObject(url, body, Map.class);
        } catch (Exception e) {
            return Map.of("decision", "NEEDS_HUMAN", "reason", "AI 服务不可用，回退人工审核");
        }
    }

    private Map<String, Object> overrideAiReason(Map<String, Object> ai, String reason) {
        java.util.HashMap<String, Object> result = ai == null ? new java.util.HashMap<>() : new java.util.HashMap<>(ai);
        result.put("reason", reason);
        return result;
    }

    private boolean shouldRequireHumanSecondReview(String resourceType, String payload) {
        String rt = resourceType == null ? "" : resourceType.trim().toUpperCase();
        boolean checkImageResource = RESOURCE_ARTICLE.equals(rt)
                || RESOURCE_COLUMN.equals(rt)
                || RESOURCE_KNOWLEDGE_BASE.equals(rt);
        if (!checkImageResource) return false;
        return hasImageInPayload(payload);
    }

    private boolean hasImageInPayload(String payload) {
        String s = payload == null ? "" : payload.trim();
        if (s.isEmpty()) return false;
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(s);
            if (node != null) {
                String cover = node.path("cover").asText("");
                if (!cover.isBlank()) return true;
                String text = node.path("text").asText("");
                if (containsCoverLine(text)) return true;
            }
        } catch (Exception ignored) {
            // non-json
        }
        return containsCoverLine(s);
    }

    private boolean containsCoverLine(String text) {
        if (text == null || text.isBlank()) return false;
        for (String line : text.split("\n")) {
            String t = line.trim();
            if (!t.startsWith("cover=")) continue;
            String v = t.substring("cover=".length()).trim();
            if (!v.isBlank()) return true;
        }
        return false;
    }

    private String aiDecision(Map<String, Object> ai) {
        if (ai == null) return "NEEDS_HUMAN";
        Object d = ai.get("decision");
        if (d == null) return "NEEDS_HUMAN";
        String s = Objects.toString(d, "").trim().toUpperCase();
        if ("PASS".equals(s) || "REJECT".equals(s) || "NEEDS_HUMAN".equals(s)) return s;
        return "NEEDS_HUMAN";
    }

    private String safeJson(Map<String, Object> m) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(m);
        } catch (Exception e) {
            return null;
        }
    }

    private ModerationTaskVO toVO(ModerationTask t) {
        ModerationTaskVO vo = new ModerationTaskVO();
        vo.setId(t.getId());
        vo.setResourceType(t.getResourceType());
        vo.setResourceId(t.getResourceId());
        vo.setResourceTitle(resolveResourceTitle(t));
        vo.setOwnerUserId(t.getOwnerUserId());
        vo.setOwnerUsername(resolveOwnerUsername(t.getOwnerUserId()));
        vo.setOwnerAvatar(resolveOwnerAvatar(t.getOwnerUserId()));
        vo.setStatus(t.getStatus());
        vo.setAiDecision(t.getAiDecision());
        vo.setAiDetail(t.getAiDetail());
        vo.setAiReviewedAt(t.getAiReviewedAt());
        vo.setHumanReviewerId(t.getHumanReviewerId());
        vo.setHumanReviewerName(resolveHumanReviewerName(t));
        vo.setHumanDecision(t.getHumanDecision());
        vo.setHumanNote(t.getHumanNote());
        vo.setHumanReviewedAt(t.getHumanReviewedAt());
        vo.setPayloadSnapshot(t.getPayloadSnapshot());
        vo.setCreatedAt(t.getCreatedAt());
        vo.setUpdatedAt(t.getUpdatedAt());
        return vo;
    }

    private String resolveHumanReviewerName(ModerationTask t) {
        if (t == null) return "";
        Long reviewerId = t.getHumanReviewerId();
        if (reviewerId != null) {
            ModerationUser reviewer = moderationUserMapper.selectById(reviewerId);
            if (reviewer != null) {
                if (reviewer.getNickname() != null && !reviewer.getNickname().isBlank()) return reviewer.getNickname();
                if (reviewer.getUsername() != null && !reviewer.getUsername().isBlank()) return reviewer.getUsername();
            }
            return "审核员";
        }
        if (STATUS_APPROVED.equals(t.getStatus()) || STATUS_REJECTED.equals(t.getStatus())) {
            return "AI审核";
        }
        return "";
    }

    private String resolveOwnerUsername(Long userId) {
        if (userId == null) return "";
        ModerationUser u = moderationUserMapper.selectById(userId);
        if (u == null) return "";
        if (u.getNickname() != null && !u.getNickname().isBlank()) return u.getNickname();
        if (u.getUsername() != null && !u.getUsername().isBlank()) return u.getUsername();
        return "";
    }

    private String resolveOwnerAvatar(Long userId) {
        if (userId == null) return "";
        ModerationUser u = moderationUserMapper.selectById(userId);
        if (u == null || u.getAvatar() == null) return "";
        return u.getAvatar();
    }

    private String resolveResourceTitle(ModerationTask task) {
        String rt = task.getResourceType();
        if (RESOURCE_ARTICLE.equals(rt) || RESOURCE_KNOWLEDGE_DOC.equals(rt)) {
            Content c = contentMapper.selectById(task.getResourceId());
            if (c != null && c.getTitle() != null && !c.getTitle().isBlank()) return c.getTitle();
            String fromPayload = extractTitleFromPayload(task.getPayloadSnapshot());
            return fromPayload != null ? fromPayload : "";
        }
        if (RESOURCE_COMMENT.equals(rt)) {
            ModerationComment c = moderationCommentMapper.selectById(task.getResourceId());
            if (c == null || c.getBody() == null) return "";
            String s = c.getBody().trim();
            return s.length() > 32 ? s.substring(0, 32) + "..." : s;
        }
        if (RESOURCE_USER_PROFILE.equals(rt)) {
            ModerationUser u = moderationUserMapper.selectById(task.getResourceId());
            return u != null && u.getUsername() != null ? u.getUsername() + " 的资料" : "个人资料";
        }
        if (RESOURCE_COLUMN.equals(rt)) {
            Column col = columnMapper.selectById(task.getResourceId());
            return col != null && col.getName() != null ? col.getName() : "";
        }
        if (RESOURCE_KNOWLEDGE_BASE.equals(rt)) {
            KnowledgeBase kb = knowledgeBaseMapper.selectById(task.getResourceId());
            return kb != null && kb.getName() != null ? kb.getName() : "";
        }
        return "";
    }

    private String extractTitleFromPayload(String payloadSnapshot) {
        String payload = payloadSnapshot == null ? "" : payloadSnapshot.trim();
        if (payload.isEmpty()) return null;
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(payload);
            if (node != null && node.has("title")) {
                String t = node.path("title").asText("");
                if (!t.isBlank()) return t.trim();
            }
            if (node != null && node.has("text")) {
                String text = node.path("text").asText("");
                String kvTitle = extractTitleFromKvText(text);
                if (kvTitle != null) return kvTitle;
            }
        } catch (Exception ignored) {
            // continue below
        }
        return extractTitleFromKvText(payload);
    }

    private String extractTitleFromKvText(String text) {
        if (text == null || text.isBlank()) return null;
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("title=")) {
                String v = trimmed.substring("title=".length()).trim();
                return v.isBlank() ? null : v;
            }
        }
        return null;
    }

    private String extractNicknameFromPayload(String payloadSnapshot) {
        String payload = payloadSnapshot == null ? "" : payloadSnapshot.trim();
        if (payload.isEmpty()) return null;
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(payload);
            if (node != null && node.has("nickname")) {
                String n = node.path("nickname").asText("");
                if (!n.isBlank()) return n.trim();
            }
            if (node != null && node.has("text")) {
                return extractFieldFromKvText(node.path("text").asText(""), "nickname");
            }
        } catch (Exception ignored) {
            // continue below
        }
        return extractFieldFromKvText(payload, "nickname");
    }

    private String extractBodyPreviewFromPayload(String payloadSnapshot) {
        String payload = payloadSnapshot == null ? "" : payloadSnapshot.trim();
        if (payload.isEmpty()) return null;
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(payload);
            if (node != null && node.has("body")) {
                String b = node.path("body").asText("");
                if (!b.isBlank()) return b.trim();
            }
            if (node != null && node.has("text")) {
                String v = extractFieldFromKvText(node.path("text").asText(""), "body");
                if (v != null) return v;
            }
        } catch (Exception ignored) {
            // continue below
        }
        String fromKv = extractFieldFromKvText(payload, "body");
        if (fromKv != null) return fromKv;
        return payload;
    }

    private String extractFieldFromKvText(String text, String field) {
        if (text == null || text.isBlank() || field == null || field.isBlank()) return null;
        String prefix = field + "=";
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) {
                String v = trimmed.substring(prefix.length()).trim();
                return v.isBlank() ? null : v;
            }
        }
        return null;
    }

    private String shorten(String text, int maxLen) {
        if (text == null) return "";
        String s = text.replace('\n', ' ').trim();
        if (s.length() <= maxLen) return s;
        return s.substring(0, Math.max(1, maxLen)) + "...";
    }
}
