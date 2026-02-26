package com.blog.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.content.dto.ContentListItemVO;
import com.blog.content.dto.ContentMeStatsVO;
import com.blog.content.dto.ContentsMeResponse;
import com.blog.content.dto.SaveDraftRequest;
import com.blog.content.dto.SaveDraftResponse;
import com.blog.content.entity.Content;
import com.blog.content.entity.ContentCollection;
import com.blog.content.entity.ContentTag;
import com.blog.content.entity.Tag;
import com.blog.content.mapper.ContentCollectionMapper;
import com.blog.content.mapper.ContentMapper;
import com.blog.content.mapper.ContentTagMapper;
import com.blog.content.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentService {

    private static final String TYPE_BLOG = "BLOG";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String TITLE_EMPTY = "[无标题]";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final int MAX_TAG_NAMES = 5;

    private final ContentMapper contentMapper;
    private final ContentTagMapper contentTagMapper;
    private final TagMapper tagMapper;
    private final ContentCollectionMapper contentCollectionMapper;
    private final RestTemplate restTemplate;

    @Value("${app.interaction-service-url:http://localhost:8085}")
    private String interactionServiceUrl;

    public ContentsMeResponse listMyContents(Long userId, int page, int pageSize,
                                            String visibility, String status, String sortBy, String order) {
        LambdaQueryWrapper<Content> q = new LambdaQueryWrapper<>();
        q.eq(Content::getUserId, userId)
                .eq(Content::getType, TYPE_BLOG);
        if (visibility != null && !visibility.isBlank()) {
            q.eq(Content::getVisibility, visibility.toUpperCase());
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            q.eq(Content::getStatus, status.toUpperCase());
        }
        boolean asc = "asc".equalsIgnoreCase(order);
        if ("likes".equalsIgnoreCase(sortBy)) {
            q.orderBy(true, asc, Content::getLikeCount);
            q.orderBy(true, false, Content::getCreatedAt);
        } else if ("views".equalsIgnoreCase(sortBy)) {
            q.orderBy(true, asc, Content::getViewCount);
            q.orderBy(true, false, Content::getCreatedAt);
        } else {
            q.orderBy(true, asc, Content::getCreatedAt);
        }
        Page<Content> p = contentMapper.selectPage(new Page<>(page, pageSize), q);
        List<ContentListItemVO> list = p.getRecords().stream()
                .map(this::toListItemVO)
                .collect(Collectors.toList());
        ContentsMeResponse res = new ContentsMeResponse();
        res.setList(list);
        res.setTotal(p.getTotal());
        return res;
    }

    /**
     * 按 ID 列表批量返回内容摘要，顺序与请求 ids 一致；不存在的 ID 不返回。
     */
    public List<ContentListItemVO> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        List<Content> list = contentMapper.selectBatchIds(ids);
        if (list.isEmpty()) return Collections.emptyList();
        List<ContentListItemVO> voList = list.stream().map(this::toListItemVO).collect(Collectors.toList());
        // 按请求 ids 顺序排列
        List<ContentListItemVO> ordered = new ArrayList<>(voList.size());
        for (Long id : ids) {
            voList.stream().filter(vo -> id.equals(vo.getId())).findFirst().ifPresent(ordered::add);
        }
        return ordered;
    }

    /**
     * 创作者中心统计：总阅读/总点赞/收藏及昨日增长（昨日阅读暂无按日日志固定为 0）
     */
    public ContentMeStatsVO getStats(Long userId) {
        List<Content> list = contentMapper.selectList(
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getUserId, userId)
                        .eq(Content::getType, TYPE_BLOG));
        long totalView = list.stream().mapToLong(c -> c.getViewCount() != null ? c.getViewCount() : 0).sum();
        long totalLike = list.stream().mapToLong(c -> c.getLikeCount() != null ? c.getLikeCount() : 0).sum();
        long totalCollection = list.stream().mapToLong(c -> c.getCollectionCount() != null ? c.getCollectionCount() : 0).sum();
        List<Long> contentIds = list.stream().map(Content::getId).collect(Collectors.toList());

        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long yesterdayCollection = 0L;
        if (!contentIds.isEmpty()) {
            Long cnt = contentCollectionMapper.selectCount(
                    new LambdaQueryWrapper<ContentCollection>()
                            .in(ContentCollection::getContentId, contentIds)
                            .ge(ContentCollection::getCreatedAt, yesterdayStart)
                            .lt(ContentCollection::getCreatedAt, todayStart));
            yesterdayCollection = cnt != null ? cnt : 0L;
        }

        long yesterdayLike = 0L;
        if (!contentIds.isEmpty()) {
            try {
                var request = new java.util.HashMap<String, Object>();
                request.put("contentIds", contentIds);
                var response = restTemplate.postForObject(
                        interactionServiceUrl + "/api/content-likes/yesterday-count",
                        request,
                        java.util.Map.class);
                if (response != null && response.get("count") != null) {
                    Object count = response.get("count");
                    yesterdayLike = count instanceof Number ? ((Number) count).longValue() : 0L;
                }
            } catch (Exception ignored) {
                // 互动服务不可用时昨日点赞增长记为 0
            }
        }

        ContentMeStatsVO vo = new ContentMeStatsVO();
        vo.setTotalViewCount(totalView);
        vo.setTotalLikeCount(totalLike);
        vo.setTotalCollectionCount(totalCollection);
        vo.setYesterdayViewDelta(0L);
        vo.setYesterdayLikeDelta(yesterdayLike);
        vo.setYesterdayCollectionDelta(yesterdayCollection);
        return vo;
    }

    /**
     * 保存草稿：正文不为空才允许保存；标题为空则存为 [无标题]。标签按名称先查询，不存在则创建再关联，最多 5 个。
     */
    public SaveDraftResponse saveDraft(Long userId, SaveDraftRequest request) {
        String body = request.getBody() != null ? request.getBody().trim() : "";
        if (body.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "正文不能为空");
        }
        String title = request.getTitle() != null ? request.getTitle().trim() : "";
        if (title.isEmpty()) title = TITLE_EMPTY;

        Content c = new Content();
        c.setUserId(userId);
        c.setType(TYPE_BLOG);
        c.setTitle(title);
        c.setBody(body);
        c.setSummary(request.getSummary() != null ? request.getSummary().trim() : null);
        c.setCover(request.getCover() != null ? request.getCover().trim() : null);
        c.setColumnId(request.getColumnId());
        c.setStatus(STATUS_DRAFT);
        String articleType = request.getArticleType() != null && !request.getArticleType().isBlank()
                ? request.getArticleType().trim().toUpperCase() : "ORIGINAL";
        if (!"ORIGINAL".equals(articleType) && !"REPRINT".equals(articleType) && !"TRANSLATED".equals(articleType)) articleType = "ORIGINAL";
        c.setArticleType(articleType);
        String creationStatement = request.getCreationStatement() != null && !request.getCreationStatement().isBlank()
                ? request.getCreationStatement().trim().toLowerCase() : "none";
        c.setCreationStatement(creationStatement);
        String visibility = request.getVisibility() != null && !request.getVisibility().isBlank()
                ? request.getVisibility().trim().toUpperCase() : "ALL";
        if (!"ALL".equals(visibility) && !"SELF".equals(visibility) && !"FANS".equals(visibility)) visibility = "ALL";
        c.setVisibility(visibility);
        c.setLikeCount(0);
        c.setCollectionCount(0);
        c.setViewCount(0);
        c.setCommentCount(0);
        contentMapper.insert(c);

        List<String> tagNames = request.getTagNames();
        if (tagNames != null && !tagNames.isEmpty()) {
            int limit = Math.min(tagNames.size(), MAX_TAG_NAMES);
            for (int i = 0; i < limit; i++) {
                String name = tagNames.get(i);
                if (name == null || (name = name.trim()).isEmpty()) continue;
                Long tagId = ensureTagByName(name);
                if (tagId != null) {
                    ContentTag ct = new ContentTag();
                    ct.setContentId(c.getId());
                    ct.setTagId(tagId);
                    contentTagMapper.insert(ct);
                }
            }
        }

        Content saved = contentMapper.selectById(c.getId());
        SaveDraftResponse res = new SaveDraftResponse();
        res.setId(c.getId());
        res.setTitle(c.getTitle());
        res.setStatus(STATUS_DRAFT);
        res.setCreatedAt(saved != null && saved.getCreatedAt() != null ? saved.getCreatedAt().format(ISO_FORMAT) : null);
        return res;
    }

    /** 按名称查询标签，不存在则插入（is_main=0）并返回 id */
    private Long ensureTagByName(String name) {
        Tag existing = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name).last("LIMIT 1"));
        if (existing != null) return existing.getId();
        Tag t = new Tag();
        t.setName(name);
        t.setIsMain(0);
        tagMapper.insert(t);
        return t.getId();
    }

    private ContentListItemVO toListItemVO(Content c) {
        ContentListItemVO vo = new ContentListItemVO();
        vo.setId(c.getId());
        vo.setTitle(c.getTitle());
        vo.setSummary(c.getSummary());
        vo.setCover(c.getCover());
        vo.setStatus(c.getStatus());
        vo.setArticleType(c.getArticleType());
        vo.setViewCount(c.getViewCount() != null ? c.getViewCount() : 0);
        vo.setLikeCount(c.getLikeCount() != null ? c.getLikeCount() : 0);
        vo.setCollectionCount(c.getCollectionCount() != null ? c.getCollectionCount() : 0);
        vo.setCommentCount(c.getCommentCount() != null ? c.getCommentCount() : 0);
        vo.setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt().format(DATE_FORMAT) : null);
        return vo;
    }
}
