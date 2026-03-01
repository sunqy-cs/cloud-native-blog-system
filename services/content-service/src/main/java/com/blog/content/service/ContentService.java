package com.blog.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.content.dto.ContentDetailVO;
import com.blog.content.dto.ContentListItemVO;
import com.blog.content.dto.ContentViewVO;
import com.blog.content.dto.ContentMeStatsVO;
import com.blog.content.dto.ContentsMeResponse;
import com.blog.content.dto.PublishResponse;
import com.blog.content.dto.SaveDraftRequest;
import com.blog.content.dto.SaveDraftResponse;
import com.blog.content.entity.Content;
import com.blog.content.entity.ContentCollection;
import com.blog.content.entity.ContentTag;
import com.blog.content.entity.ContentView;
import com.blog.content.entity.Tag;
import com.blog.content.mapper.ContentCollectionMapper;
import com.blog.content.mapper.ContentMapper;
import com.blog.content.mapper.ContentTagMapper;
import com.blog.content.mapper.ContentViewMapper;
import com.blog.content.mapper.TagMapper;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentService {

    private static final String TYPE_BLOG = "BLOG";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String VISIBILITY_ALL = "ALL";
    private static final String VISIBILITY_SELF = "SELF";
    private static final String VISIBILITY_FANS = "FANS";
    private static final String TITLE_EMPTY = "[无标题]";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final int MAX_TAG_NAMES = 5;

    private final ContentMapper contentMapper;
    private final ContentTagMapper contentTagMapper;
    private final TagMapper tagMapper;
    private final ContentCollectionMapper contentCollectionMapper;
    private final ContentViewMapper contentViewMapper;
    private final RestTemplate restTemplate;

    @Value("${app.interaction-service-url:http://localhost:8085}")
    private String interactionServiceUrl;

    /** 调用 interaction-service 判断 followerId 是否关注了 followeeId；失败或未关注返回 false */
    private boolean isFollowingRemote(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) return false;
        try {
            String url = interactionServiceUrl.replaceFirst("/$", "") + "/api/follow/check?followeeId=" + followeeId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(followerId));
            ResponseEntity<Map<String, Boolean>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Boolean>>() {});
            return Boolean.TRUE.equals(resp.getBody() != null ? resp.getBody().get("following") : false);
        } catch (Exception e) {
            return false;
        }
    }

    public ContentsMeResponse listMyContents(Long userId, int page, int pageSize,
                                            String visibility, String status, String sortBy, String order, String keyword, Long columnId) {
        LambdaQueryWrapper<Content> q = new LambdaQueryWrapper<>();
        q.eq(Content::getUserId, userId)
                .eq(Content::getType, TYPE_BLOG);
        if (visibility != null && !visibility.isBlank()) {
            q.eq(Content::getVisibility, visibility.toUpperCase());
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            q.eq(Content::getStatus, status.toUpperCase());
        }
        if (columnId != null) {
            q.eq(Content::getColumnId, columnId);
        }
        if (keyword != null && !keyword.isBlank()) {
            List<Long> rawIds = contentIdsMatchingTagKeyword(keyword);
            final List<Long> contentIdsByTag = rawIds.isEmpty() ? Collections.emptyList()
                    : contentMapper.selectList(new LambdaQueryWrapper<Content>().in(Content::getId, rawIds).eq(Content::getUserId, userId).eq(Content::getType, TYPE_BLOG))
                            .stream().map(Content::getId).collect(Collectors.toList());
            if (contentIdsByTag.isEmpty()) {
                q.and(w -> w.like(Content::getTitle, keyword).or().like(Content::getSummary, keyword));
            } else {
                q.and(w -> w.like(Content::getTitle, keyword).or().like(Content::getSummary, keyword).or().in(Content::getId, contentIdsByTag));
            }
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
        if (keyword != null && !keyword.isBlank() && !list.isEmpty()) {
            List<Long> ids = list.stream().map(ContentListItemVO::getId).collect(Collectors.toList());
            Map<Long, List<String>> tagNamesMap = getTagNamesByContentIds(ids);
            list.forEach(vo -> vo.setTagNames(tagNamesMap.get(vo.getId())));
        }
        ContentsMeResponse res = new ContentsMeResponse();
        res.setList(list);
        res.setTotal(p.getTotal());
        return res;
    }

    /**
     * 公开推荐列表：已发布博客，可选按主标签、按用户/多用户筛选，按时间/点赞排序。
     * userId 单用户即「TA的博客」；userIds 多用户即「关注流」。
     * 当按用户筛选时根据 visibility 与 currentUserId 过滤：ALL 所有人可见，SELF 仅作者，FANS 仅作者与粉丝。
     */
    public ContentsMeResponse listPublic(Long mainTagId, Long userId, List<Long> userIds, Long columnId, int page, int pageSize, String sortBy, String order, Long currentUserId) {
        LambdaQueryWrapper<Content> q = new LambdaQueryWrapper<>();
        q.eq(Content::getType, TYPE_BLOG).eq(Content::getStatus, STATUS_PUBLISHED);
        if (userIds != null && !userIds.isEmpty()) {
            q.in(Content::getUserId, userIds);
        } else if (userId != null) {
            q.eq(Content::getUserId, userId);
            if (currentUserId == null) {
                q.eq(Content::getVisibility, VISIBILITY_ALL);
            } else if (!currentUserId.equals(userId)) {
                q.and(w -> w.eq(Content::getVisibility, VISIBILITY_ALL).or().eq(Content::getVisibility, VISIBILITY_FANS));
            }
        }
        if (columnId != null) {
            q.eq(Content::getColumnId, columnId);
        }
        if (mainTagId != null) {
            List<Long> contentIdsWithTag = contentTagMapper.selectList(
                    new LambdaQueryWrapper<ContentTag>().eq(ContentTag::getTagId, mainTagId))
                    .stream().map(ContentTag::getContentId).distinct().collect(Collectors.toList());
            if (contentIdsWithTag.isEmpty()) {
                ContentsMeResponse empty = new ContentsMeResponse();
                empty.setList(Collections.emptyList());
                empty.setTotal(0L);
                return empty;
            }
            q.in(Content::getId, contentIdsWithTag);
        }
        boolean asc = "asc".equalsIgnoreCase(order);
        if ("likes".equalsIgnoreCase(sortBy)) {
            q.orderBy(true, asc, Content::getLikeCount).orderBy(true, false, Content::getCreatedAt);
        } else if ("views".equalsIgnoreCase(sortBy)) {
            q.orderBy(true, asc, Content::getViewCount).orderBy(true, false, Content::getCreatedAt);
        } else {
            q.orderBy(true, asc, Content::getCreatedAt);
        }
        Page<Content> p = contentMapper.selectPage(new Page<>(page, pageSize), q);
        List<Content> records = p.getRecords();
        if (userIds != null && !userIds.isEmpty() && !records.isEmpty()) {
            records = records.stream()
                    .filter(c -> filterVisibilityForCurrentUser(c, currentUserId))
                    .collect(Collectors.toList());
        } else if (userId != null && currentUserId != null && !currentUserId.equals(userId)) {
            boolean following = isFollowingRemote(currentUserId, userId);
            records = records.stream()
                    .filter(c -> VISIBILITY_ALL.equals(c.getVisibility()) || (VISIBILITY_FANS.equals(c.getVisibility()) && following))
                    .collect(Collectors.toList());
        }
        List<ContentListItemVO> list = records.stream().map(this::toListItemVO).collect(Collectors.toList());
        ContentsMeResponse res = new ContentsMeResponse();
        res.setList(list);
        res.setTotal(p.getTotal());
        return res;
    }

    /** 多用户列表时按 visibility 过滤：未登录仅 ALL；本人内容可见；他人内容 ALL 或（FANS 且已关注） */
    private boolean filterVisibilityForCurrentUser(Content c, Long currentUserId) {
        if (currentUserId == null) return VISIBILITY_ALL.equals(c.getVisibility());
        if (currentUserId.equals(c.getUserId())) return true;
        return VISIBILITY_ALL.equals(c.getVisibility()) || (VISIBILITY_FANS.equals(c.getVisibility()) && isFollowingRemote(currentUserId, c.getUserId()));
    }

    /** 按标签名模糊匹配得到的内容 ID 列表（当前用户博客） */
    private List<Long> contentIdsMatchingTagKeyword(String keyword) {
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>().like(Tag::getName, keyword));
        if (tags.isEmpty()) return Collections.emptyList();
        List<Long> tagIds = tags.stream().map(Tag::getId).collect(Collectors.toList());
        List<ContentTag> ctList = contentTagMapper.selectList(new LambdaQueryWrapper<ContentTag>().in(ContentTag::getTagId, tagIds));
        return ctList.stream().map(ContentTag::getContentId).distinct().collect(Collectors.toList());
    }

    /** 批量获取内容 ID 对应的标签名称列表（顺序按 content_tag 关联顺序） */
    private Map<Long, List<String>> getTagNamesByContentIds(List<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) return Collections.emptyMap();
        List<ContentTag> ctList = contentTagMapper.selectList(new LambdaQueryWrapper<ContentTag>().in(ContentTag::getContentId, contentIds));
        if (ctList.isEmpty()) return Collections.emptyMap();
        List<Long> tagIds = ctList.stream().map(ContentTag::getTagId).distinct().collect(Collectors.toList());
        List<Tag> tags = tagMapper.selectBatchIds(tagIds);
        Map<Long, String> tagIdToName = tags.stream().collect(Collectors.toMap(Tag::getId, t -> t.getName() != null ? t.getName() : "", (a, b) -> a));
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (Long cid : contentIds) {
            List<String> names = ctList.stream()
                    .filter(ct -> cid.equals(ct.getContentId()))
                    .map(ct -> tagIdToName.get(ct.getTagId()))
                    .filter(n -> n != null)
                    .collect(Collectors.toList());
            if (!names.isEmpty()) result.put(cid, names);
        }
        return result;
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

    private static final int HOT_POOL_SIZE = 2000;

    /**
     * 热榜：engagement = 1*log(阅读+1)+3*点赞+5*收藏+8*评论，time_decay = 1/(1+小时/12)，hot_score = engagement * time_decay，按 hot_score 降序分页。
     */
    public ContentsMeResponse listHot(int page, int pageSize) {
        LambdaQueryWrapper<Content> q = new LambdaQueryWrapper<>();
        q.eq(Content::getType, TYPE_BLOG).eq(Content::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Content::getCreatedAt);
        Page<Content> pool = contentMapper.selectPage(new Page<>(1, HOT_POOL_SIZE), q);
        List<Content> list = pool.getRecords();
        if (list.isEmpty()) {
            ContentsMeResponse res = new ContentsMeResponse();
            res.setList(Collections.emptyList());
            res.setTotal(0L);
            return res;
        }
        LocalDateTime now = LocalDateTime.now();
        List<ContentWithScore> withScores = list.stream()
                .map(c -> {
                    int views = c.getViewCount() != null ? c.getViewCount() : 0;
                    int likes = c.getLikeCount() != null ? c.getLikeCount() : 0;
                    int collections = c.getCollectionCount() != null ? c.getCollectionCount() : 0;
                    int comments = c.getCommentCount() != null ? c.getCommentCount() : 0;
                    double engagement = 1.0 * Math.log(views + 1) + 3.0 * likes + 5.0 * collections + 8.0 * comments;
                    long hours = c.getCreatedAt() != null && !c.getCreatedAt().isAfter(now)
                            ? ChronoUnit.HOURS.between(c.getCreatedAt(), now)
                            : 0;
                    double timeDecay = 1.0 / (1.0 + hours / 12.0);
                    double hotScore = engagement * timeDecay;
                    return new ContentWithScore(c, hotScore);
                })
                .sorted((a, b) -> Double.compare(b.hotScore, a.hotScore))
                .collect(Collectors.toList());
        int total = withScores.size();
        int from = (page - 1) * pageSize;
        if (from >= total) {
            ContentsMeResponse res = new ContentsMeResponse();
            res.setList(Collections.emptyList());
            res.setTotal((long) total);
            return res;
        }
        int to = Math.min(from + pageSize, total);
        List<Content> pageContents = withScores.subList(from, to).stream().map(ws -> ws.content).collect(Collectors.toList());
        List<Long> contentIds = pageContents.stream().map(Content::getId).collect(Collectors.toList());
        Map<Long, List<String>> tagNamesMap = getTagNamesByContentIds(contentIds);
        List<ContentListItemVO> voList = withScores.subList(from, to).stream()
                .map(ws -> {
                    ContentListItemVO vo = toListItemVO(ws.content);
                    vo.setHotScore(ws.hotScore);
                    vo.setTagNames(tagNamesMap.get(ws.content.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
        ContentsMeResponse res = new ContentsMeResponse();
        res.setList(voList);
        res.setTotal((long) total);
        return res;
    }

    private static class ContentWithScore {
        final Content content;
        final double hotScore;

        ContentWithScore(Content content, double hotScore) {
            this.content = content;
            this.hotScore = hotScore;
        }
    }

    /**
     * 公开阅读：已发布博客，返回正文、阅读数、点赞数、作者 id 等；不存在或未发布则 404。
     * 按 visibility 校验：ALL 所有人可见，SELF 仅作者，FANS 仅作者与粉丝；无权限返回 404。
     * 阅读量仅在该用户首次阅读该文章时 +1（依赖 content_view 中间表去重）；未登录不增加阅读量。
     */
    @Transactional
    public ContentViewVO getForView(Long id, Long userId) {
        if (id == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在");
        Content c = contentMapper.selectById(id);
        if (c == null || !TYPE_BLOG.equals(c.getType()) || !STATUS_PUBLISHED.equals(c.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或未发布");
        }
        String vis = c.getVisibility() != null ? c.getVisibility().toUpperCase() : VISIBILITY_ALL;
        if (VISIBILITY_SELF.equals(vis)) {
            if (userId == null || !userId.equals(c.getUserId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或未发布");
            }
        } else if (VISIBILITY_FANS.equals(vis)) {
            if (userId == null || (!userId.equals(c.getUserId()) && !isFollowingRemote(userId, c.getUserId()))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或未发布");
            }
        }
        if (userId != null) {
            long exists = contentViewMapper.selectCount(
                    new LambdaQueryWrapper<ContentView>()
                            .eq(ContentView::getUserId, userId)
                            .eq(ContentView::getContentId, id));
            if (exists == 0) {
                ContentView cv = new ContentView();
                cv.setUserId(userId);
                cv.setContentId(id);
                cv.setCreatedAt(LocalDateTime.now());
                contentViewMapper.insert(cv);
                LambdaUpdateWrapper<Content> u = new LambdaUpdateWrapper<>();
                u.eq(Content::getId, id).setSql("view_count = view_count + 1");
                contentMapper.update(null, u);
                c.setViewCount(c.getViewCount() != null ? c.getViewCount() + 1 : 1);
            }
        }
        ContentViewVO vo = new ContentViewVO();
        vo.setId(c.getId());
        vo.setTitle(c.getTitle());
        vo.setBody(c.getBody());
        vo.setSummary(c.getSummary());
        vo.setCover(c.getCover());
        vo.setColumnId(c.getColumnId());
        vo.setArticleType(c.getArticleType());
        vo.setCreationStatement(c.getCreationStatement());
        vo.setVisibility(c.getVisibility());
        vo.setViewCount(c.getViewCount());
        vo.setLikeCount(c.getLikeCount() != null ? c.getLikeCount() : 0);
        vo.setCommentCount(c.getCommentCount() != null ? c.getCommentCount() : 0);
        vo.setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt().format(ISO_FORMAT) : null);
        vo.setUserId(c.getUserId());
        List<ContentTag> ctList = contentTagMapper.selectList(new LambdaQueryWrapper<ContentTag>().eq(ContentTag::getContentId, id));
        if (!ctList.isEmpty()) {
            List<Long> tagIds = ctList.stream().map(ContentTag::getTagId).collect(Collectors.toList());
            List<Tag> tags = tagMapper.selectBatchIds(tagIds);
            Map<Long, String> tagIdToName = tags.stream().collect(Collectors.toMap(Tag::getId, t -> t.getName() != null ? t.getName() : "", (a, b) -> a));
            List<String> tagNames = ctList.stream().map(ct -> tagIdToName.get(ct.getTagId())).filter(n -> n != null).collect(Collectors.toList());
            vo.setTagNames(tagNames);
        }
        return vo;
    }

    /**
     * 获取编辑用内容详情（仅当前用户本人的内容）
     */
    public ContentDetailVO getForEdit(Long userId, Long id) {
        if (id == null) return null;
        Content c = contentMapper.selectById(id);
        if (c == null || !userId.equals(c.getUserId()) || !TYPE_BLOG.equals(c.getType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或无权编辑");
        }
        ContentDetailVO vo = new ContentDetailVO();
        vo.setId(c.getId());
        vo.setTitle(c.getTitle());
        vo.setBody(c.getBody());
        vo.setSummary(c.getSummary());
        vo.setCover(c.getCover());
        vo.setColumnId(c.getColumnId());
        vo.setArticleType(c.getArticleType());
        vo.setCreationStatement(c.getCreationStatement() != null ? c.getCreationStatement() : "none");
        vo.setVisibility(c.getVisibility() != null ? c.getVisibility() : "ALL");
        List<ContentTag> ctList = contentTagMapper.selectList(
                new LambdaQueryWrapper<ContentTag>().eq(ContentTag::getContentId, id));
        List<String> tagNames = new ArrayList<>();
        if (!ctList.isEmpty()) {
            List<Long> tagIds = ctList.stream().map(ContentTag::getTagId).collect(Collectors.toList());
            List<Tag> tags = tagMapper.selectBatchIds(tagIds);
            tags.sort((a, b) -> {
                int am = a.getIsMain() != null && a.getIsMain() == 1 ? 0 : 1;
                int bm = b.getIsMain() != null && b.getIsMain() == 1 ? 0 : 1;
                if (am != bm) return am - bm;
                return Long.compare(tagIds.indexOf(a.getId()), tagIds.indexOf(b.getId()));
            });
            tagNames = tags.stream().map(Tag::getName).filter(n -> n != null).collect(Collectors.toList());
        }
        vo.setTagNames(tagNames);
        return vo;
    }

    /**
     * 发布博客：将草稿状态改为已发布。仅作者本人可操作，且当前必须为 DRAFT。
     */
    public PublishResponse publish(Long userId, Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "内容 ID 不能为空");
        }
        Content c = contentMapper.selectById(id);
        if (c == null || !userId.equals(c.getUserId()) || !TYPE_BLOG.equals(c.getType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或无权操作");
        }
        if (STATUS_PUBLISHED.equals(c.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该内容已发布");
        }
        c.setStatus(STATUS_PUBLISHED);
        c.setUpdatedAt(LocalDateTime.now());
        contentMapper.updateById(c);
        PublishResponse res = new PublishResponse();
        res.setId(c.getId());
        res.setTitle(c.getTitle());
        res.setStatus(STATUS_PUBLISHED);
        res.setPublishedAt(c.getUpdatedAt() != null ? c.getUpdatedAt().format(ISO_FORMAT) : null);
        return res;
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

        Long requestId = request.getId();
        Content c;
        if (requestId != null) {
            c = contentMapper.selectById(requestId);
            if (c == null || !userId.equals(c.getUserId()) || !TYPE_BLOG.equals(c.getType())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或无权编辑");
            }
            c.setTitle(title);
            c.setBody(body);
            c.setSummary(request.getSummary() != null ? request.getSummary().trim() : null);
            c.setCover(request.getCover() != null ? request.getCover().trim() : null);
            c.setColumnId(request.getColumnId());
            c.setStatus(STATUS_DRAFT);
            String at = request.getArticleType() != null && !request.getArticleType().isBlank()
                    ? request.getArticleType().trim().toUpperCase() : "ORIGINAL";
            if (!"ORIGINAL".equals(at) && !"REPRINT".equals(at) && !"TRANSLATED".equals(at)) at = "ORIGINAL";
            c.setArticleType(at);
            c.setCreationStatement(request.getCreationStatement() != null && !request.getCreationStatement().isBlank()
                    ? request.getCreationStatement().trim().toLowerCase() : "none");
            String vis = request.getVisibility() != null && !request.getVisibility().isBlank()
                    ? request.getVisibility().trim().toUpperCase() : "ALL";
            if (!"ALL".equals(vis) && !"SELF".equals(vis) && !"FANS".equals(vis)) vis = "ALL";
            c.setVisibility(vis);
            c.setUpdatedAt(LocalDateTime.now());
            contentMapper.updateById(c);
            contentTagMapper.delete(new LambdaQueryWrapper<ContentTag>().eq(ContentTag::getContentId, c.getId()));
        } else {
            c = new Content();
            c.setUserId(userId);
            c.setType(TYPE_BLOG);
            c.setTitle(title);
            c.setBody(body);
            c.setSummary(request.getSummary() != null ? request.getSummary().trim() : null);
            c.setCover(request.getCover() != null ? request.getCover().trim() : null);
            c.setColumnId(request.getColumnId());
            c.setStatus(STATUS_DRAFT);
            String at = request.getArticleType() != null && !request.getArticleType().isBlank()
                    ? request.getArticleType().trim().toUpperCase() : "ORIGINAL";
            if (!"ORIGINAL".equals(at) && !"REPRINT".equals(at) && !"TRANSLATED".equals(at)) at = "ORIGINAL";
            c.setArticleType(at);
            c.setCreationStatement(request.getCreationStatement() != null && !request.getCreationStatement().isBlank()
                    ? request.getCreationStatement().trim().toLowerCase() : "none");
            String vis = request.getVisibility() != null && !request.getVisibility().isBlank()
                    ? request.getVisibility().trim().toUpperCase() : "ALL";
            if (!"ALL".equals(vis) && !"SELF".equals(vis) && !"FANS".equals(vis)) vis = "ALL";
            c.setVisibility(vis);
            c.setLikeCount(0);
            c.setCollectionCount(0);
            c.setViewCount(0);
            c.setCommentCount(0);
            contentMapper.insert(c);
        }

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
        vo.setUserId(c.getUserId());
        vo.setTitle(c.getTitle());
        vo.setSummary(c.getSummary());
        vo.setCover(c.getCover());
        vo.setStatus(c.getStatus());
        vo.setArticleType(c.getArticleType());
        vo.setViewCount(c.getViewCount() != null ? c.getViewCount() : 0);
        vo.setLikeCount(c.getLikeCount() != null ? c.getLikeCount() : 0);
        vo.setCollectionCount(c.getCollectionCount() != null ? c.getCollectionCount() : 0);
        vo.setCommentCount(c.getCommentCount() != null ? c.getCommentCount() : 0);
        vo.setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt().format(ISO_FORMAT) : null);
        return vo;
    }
}
