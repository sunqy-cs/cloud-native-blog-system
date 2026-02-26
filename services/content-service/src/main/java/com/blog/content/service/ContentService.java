package com.blog.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.content.dto.ContentListItemVO;
import com.blog.content.dto.ContentMeStatsVO;
import com.blog.content.dto.ContentsMeResponse;
import com.blog.content.entity.Content;
import com.blog.content.entity.ContentCollection;
import com.blog.content.mapper.ContentCollectionMapper;
import com.blog.content.mapper.ContentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ContentMapper contentMapper;
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
