package com.blog.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.content.dto.ContentDetailVO;
import com.blog.content.dto.ContentListItemVO;
import com.blog.content.dto.ContentViewVO;
import com.blog.content.dto.ContentMeStatsVO;
import com.blog.content.dto.ContentsMeResponse;
import com.blog.content.dto.CreatorAnalyticsVO;
import com.blog.content.dto.PublishResponse;
import com.blog.content.dto.SaveDraftRequest;
import com.blog.content.dto.SaveDraftResponse;
import com.blog.content.dto.ModerationSubmitRequest;
import com.blog.content.entity.Content;
import com.blog.content.entity.ContentCollection;
import com.blog.content.entity.ContentReference;
import com.blog.content.entity.ContentTag;
import com.blog.content.entity.ContentView;
import com.blog.content.entity.KnowledgeBase;
import com.blog.content.entity.KnowledgeBaseContent;
import com.blog.content.entity.KnowledgeBaseFavorite;
import com.blog.content.entity.Tag;
import com.blog.content.mapper.ContentCollectionMapper;
import com.blog.content.mapper.ContentMapper;
import com.blog.content.mapper.ContentReferenceMapper;
import com.blog.content.mapper.ContentTagMapper;
import com.blog.content.mapper.ContentViewMapper;
import com.blog.content.mapper.KnowledgeBaseContentMapper;
import com.blog.content.mapper.KnowledgeBaseFavoriteMapper;
import com.blog.content.mapper.KnowledgeBaseMapper;
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
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class ContentService {

    private static final String TYPE_BLOG = "BLOG";
    private static final String TYPE_KNOWLEDGE = "KNOWLEDGE";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String VISIBILITY_ALL = "ALL";
    private static final String VISIBILITY_SELF = "SELF";
    private static final String VISIBILITY_FANS = "FANS";
    private static final String KB_VISIBILITY_PUBLIC = "PUBLIC";
    private static final String TITLE_EMPTY = "[无标题]";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final int MAX_TAG_NAMES = 5;

    private final ContentMapper contentMapper;
    private final ContentTagMapper contentTagMapper;
    private final TagMapper tagMapper;
    private final ContentCollectionMapper contentCollectionMapper;
    private final ContentReferenceMapper contentReferenceMapper;
    private final ContentViewMapper contentViewMapper;
    private final KnowledgeBaseContentMapper knowledgeBaseContentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseFavoriteMapper knowledgeBaseFavoriteMapper;
    private final RestTemplate restTemplate;
    private final KbVectorService kbVectorService;
    private final ModerationService moderationService;

    /** 双链笔记：正文中 [[id:标题]] 或 [[标题]] 的匹配 */
    private static final Pattern WIKILINK_PATTERN = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");

    @Value("${app.interaction-service-url:http://localhost:8085}")
    private String interactionServiceUrl;

    @Value("${app.search-service-url:http://localhost:8087}")
    private String searchServiceUrl;

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

    /** 发布后通知 search-service 建索引（标题、摘要、正文、标签） */
    private void notifySearchServiceIndex(Content c) {
        if (c == null || c.getId() == null) return;
        try {
            List<String> tagNames = getTagNamesByContentIds(List.of(c.getId())).getOrDefault(c.getId(), List.of());
            var body = new java.util.HashMap<String, Object>();
            body.put("id", c.getId());
            body.put("userId", c.getUserId());
            body.put("title", c.getTitle() != null ? c.getTitle() : "");
            body.put("summary", c.getSummary() != null ? c.getSummary() : "");
            body.put("body", c.getBody() != null ? c.getBody() : "");
            body.put("tagNames", tagNames);
            body.put("publishedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().format(ISO_FORMAT)
                : (c.getCreatedAt() != null ? c.getCreatedAt().format(ISO_FORMAT) : LocalDateTime.now().format(ISO_FORMAT)));
            String url = searchServiceUrl.replaceFirst("/$", "") + "/api/search/index";
            restTemplate.postForObject(url, body, Void.class);
        } catch (Exception ignored) {
            // search-service 不可用时仅记录，不影响发布
        }
    }

    /** 全量重建搜索索引：把所有已发布博客推送到 search-service，用于首次或重建索引。 */
    public int reindexAllPublishedForSearch() {
        List<Content> list = contentMapper.selectList(
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getType, TYPE_BLOG)
                        .eq(Content::getStatus, STATUS_PUBLISHED));
        for (Content c : list) {
            notifySearchServiceIndex(c);
        }
        return list.size();
    }

    /** 我的内容列表；contentType 为 null/空/BLOG 时只查博客，KNOWLEDGE 时只查知识库（内容管理页只传 BLOG，不展示知识库文件） */
    public ContentsMeResponse listMyContents(Long userId, int page, int pageSize,
                                            String visibility, String status, String sortBy, String order, String keyword, Long columnId, String contentType) {
        String typeFilter = (contentType != null && "KNOWLEDGE".equalsIgnoreCase(contentType.trim())) ? TYPE_KNOWLEDGE : TYPE_BLOG;
        LambdaQueryWrapper<Content> q = new LambdaQueryWrapper<>();
        q.eq(Content::getUserId, userId)
                .eq(Content::getType, typeFilter);
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
                    : contentMapper.selectList(new LambdaQueryWrapper<Content>().in(Content::getId, rawIds).eq(Content::getUserId, userId).eq(Content::getType, typeFilter))
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

    /** 文章被收藏时增加收藏数（收藏夹添加内容时调用） */
    public void incrementCollectionCount(Long contentId) {
        if (contentId == null) return;
        Content c = contentMapper.selectById(contentId);
        if (c == null) return;
        int count = c.getCollectionCount() != null ? c.getCollectionCount() : 0;
        c.setCollectionCount(count + 1);
        contentMapper.updateById(c);
    }

    /** 从收藏夹移除时减少收藏数 */
    public void decrementCollectionCount(Long contentId) {
        if (contentId == null) return;
        Content c = contentMapper.selectById(contentId);
        if (c == null) return;
        int count = c.getCollectionCount() != null ? c.getCollectionCount() : 0;
        if (count > 0) {
            c.setCollectionCount(count - 1);
            contentMapper.updateById(c);
        }
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
        if (c == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在");
        }
        if (TYPE_KNOWLEDGE.equals(c.getType())) {
            // 知识库文件：仅作者本人，或所在知识库为公开/已订阅时可查看
            if (!canViewKnowledgeContent(c, userId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或无权查看");
            }
            return buildContentViewVO(c, id, userId);
        }
        if (!TYPE_BLOG.equals(c.getType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或未发布");
        }
        // 已发布：所有人（按 visibility）可见；未发布草稿：仅作者本人可见（便于知识库内展示自己的博客）
        if (!STATUS_PUBLISHED.equals(c.getStatus())) {
            if (userId == null || !userId.equals(c.getUserId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或未发布");
            }
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
        return buildContentViewVO(c, id, userId);
    }

    /** 知识库文件是否可被该用户查看：作者本人，或内容所在知识库为公开/当前用户已订阅 */
    private boolean canViewKnowledgeContent(Content c, Long userId) {
        if (c == null || !TYPE_KNOWLEDGE.equals(c.getType())) return false;
        if (userId != null && userId.equals(c.getUserId())) return true;
        List<KnowledgeBaseContent> kbcList = knowledgeBaseContentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseContent>().eq(KnowledgeBaseContent::getContentId, c.getId()));
        if (kbcList.isEmpty()) return false;
        List<Long> kbIds = kbcList.stream().map(KnowledgeBaseContent::getKnowledgeBaseId).distinct().collect(Collectors.toList());
        for (Long kbId : kbIds) {
            KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
            if (kb == null) continue;
            if (KB_VISIBILITY_PUBLIC.equals(kb.getVisibility())) return true;
            if (userId != null && userId.equals(kb.getUserId())) return true;
            if (userId != null && knowledgeBaseFavoriteMapper.selectCount(
                    new LambdaQueryWrapper<KnowledgeBaseFavorite>()
                            .eq(KnowledgeBaseFavorite::getUserId, userId)
                            .eq(KnowledgeBaseFavorite::getKnowledgeBaseId, kbId)) > 0) return true;
        }
        return false;
    }

    private ContentViewVO buildContentViewVO(Content c, Long id, Long userId) {
        if (userId != null && (STATUS_PUBLISHED.equals(c.getStatus()) || TYPE_KNOWLEDGE.equals(c.getType()))) {
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
        String viewPublishedAt = c.getUpdatedAt() != null ? c.getUpdatedAt().format(ISO_FORMAT)
                : (c.getCreatedAt() != null ? c.getCreatedAt().format(ISO_FORMAT) : null);
        vo.setPublishedAt(viewPublishedAt);
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
        boolean allowedType = TYPE_BLOG.equals(c.getType()) || TYPE_KNOWLEDGE.equals(c.getType());
        if (c == null || !userId.equals(c.getUserId()) || !allowedType) {
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

    /** 仅更新内容标题（用于知识库内文件重命名等）；支持 BLOG 与 KNOWLEDGE 类型 */
    public void updateTitle(Long userId, Long id, String title) {
        if (id == null) return;
        Content c = contentMapper.selectById(id);
        if (c == null || !userId.equals(c.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或无权编辑");
        }
        if (!TYPE_BLOG.equals(c.getType()) && !"KNOWLEDGE".equals(c.getType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或无权编辑");
        }
        String t = title != null ? title.trim() : "";
        if (t.isEmpty()) t = "[无标题]";
        c.setTitle(t);
        contentMapper.updateById(c);
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
        if (moderationService.isAdmin(userId)) {
            c.setStatus(STATUS_PUBLISHED);
            c.setModerationStatus(ModerationService.STATUS_APPROVED);
            c.setUpdatedAt(LocalDateTime.now());
            contentMapper.updateById(c);
            notifySearchServiceIndex(c);
            try {
                kbVectorService.refreshEmbeddingsForContent(c.getId());
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(ContentService.class).warn("发布后刷新知识库向量失败 contentId={}", c.getId(), e);
            }
        } else {
            ModerationSubmitRequest req = new ModerationSubmitRequest();
            req.setResourceType("ARTICLE");
            req.setResourceId(c.getId());
            req.setOwnerUserId(userId);
            req.setPayloadSnapshot(buildModerationPayload(c));
            moderationService.submitTask(req);
            c = contentMapper.selectById(c.getId());
            if (STATUS_PUBLISHED.equals(c.getStatus())) {
                notifySearchServiceIndex(c);
            }
        }
        PublishResponse res = new PublishResponse();
        res.setId(c.getId());
        res.setTitle(c.getTitle());
        res.setStatus(c.getStatus());
        res.setPublishedAt(c.getUpdatedAt() != null ? c.getUpdatedAt().format(ISO_FORMAT) : null);
        return res;
    }

    private String buildModerationPayload(Content c) {
        String body = c.getBody() != null ? c.getBody() : "";
        if (body.length() > 2000) body = body.substring(0, 2000);
        return "title=" + (c.getTitle() != null ? c.getTitle() : "")
                + "\nsummary=" + (c.getSummary() != null ? c.getSummary() : "")
                + "\ncover=" + (c.getCover() != null ? c.getCover() : "")
                + "\nbody=" + body;
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

    public CreatorAnalyticsVO getCreatorAnalytics(Long userId, int days) {
        List<Content> all = contentMapper.selectList(
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getUserId, userId)
                        .eq(Content::getType, TYPE_BLOG)
                        .orderByDesc(Content::getUpdatedAt));

        CreatorAnalyticsVO vo = new CreatorAnalyticsVO();
        vo.setOverview(buildOverview(userId, all));
        vo.setTrend(buildTrend(all, days));
        vo.setTagInsights(buildTagInsights(all));
        vo.setLengthDistribution(buildLengthDistribution(all));
        vo.setTopContents(buildTopContents(all));
        vo.setHeatmap(buildHeatmap(all));
        return vo;
    }

    private CreatorAnalyticsVO.Overview buildOverview(Long userId, List<Content> all) {
        long total = all.size();
        List<Content> published = all.stream().filter(c -> STATUS_PUBLISHED.equalsIgnoreCase(c.getStatus())).collect(Collectors.toList());
        long publishedCount = published.size();
        long draftCount = Math.max(0, total - publishedCount);
        long views = all.stream().mapToLong(c -> c.getViewCount() == null ? 0 : c.getViewCount()).sum();
        long likes = all.stream().mapToLong(c -> c.getLikeCount() == null ? 0 : c.getLikeCount()).sum();
        long collections = all.stream().mapToLong(c -> c.getCollectionCount() == null ? 0 : c.getCollectionCount()).sum();
        long comments = all.stream().mapToLong(c -> c.getCommentCount() == null ? 0 : c.getCommentCount()).sum();
        long engagement = likes + collections + comments;

        CreatorAnalyticsVO.Overview o = new CreatorAnalyticsVO.Overview();
        o.setTotalContents(total);
        o.setPublishedContents(publishedCount);
        o.setDraftContents(draftCount);
        o.setTotalViews(views);
        o.setTotalLikes(likes);
        o.setTotalCollections(collections);
        o.setTotalComments(comments);
        o.setTotalEngagement(engagement);
        o.setAvgViewsPerPublished(publishedCount == 0 ? 0.0 : round2((double) views / publishedCount));
        o.setAvgEngagementPerPublished(publishedCount == 0 ? 0.0 : round2((double) engagement / publishedCount));
        o.setPublishRate(total == 0 ? 0.0 : round2((double) publishedCount / total));

        Map<String, Object> followStats = fetchFollowStats(userId);
        o.setFollowers(toLongSafe(followStats.get("followerCount")));
        o.setFollowing(toLongSafe(followStats.get("followingCount")));
        return o;
    }

    private List<CreatorAnalyticsVO.TrendPoint> buildTrend(List<Content> all, int days) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, CreatorAnalyticsVO.TrendPoint> byDate = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            CreatorAnalyticsVO.TrendPoint p = new CreatorAnalyticsVO.TrendPoint();
            p.setDate(d.toString());
            p.setPublishedCount(0);
            p.setViews(0L);
            p.setLikes(0L);
            p.setCollections(0L);
            p.setComments(0L);
            p.setScore(0.0);
            byDate.put(d, p);
        }

        for (Content c : all) {
            LocalDateTime t = c.getUpdatedAt() != null ? c.getUpdatedAt() : c.getCreatedAt();
            if (t == null) continue;
            LocalDate d = t.toLocalDate();
            CreatorAnalyticsVO.TrendPoint p = byDate.get(d);
            if (p == null) continue;
            if (STATUS_PUBLISHED.equalsIgnoreCase(c.getStatus())) {
                p.setPublishedCount(p.getPublishedCount() + 1);
            }
            long v = c.getViewCount() == null ? 0 : c.getViewCount();
            long l = c.getLikeCount() == null ? 0 : c.getLikeCount();
            long col = c.getCollectionCount() == null ? 0 : c.getCollectionCount();
            long com = c.getCommentCount() == null ? 0 : c.getCommentCount();
            p.setViews(p.getViews() + v);
            p.setLikes(p.getLikes() + l);
            p.setCollections(p.getCollections() + col);
            p.setComments(p.getComments() + com);
            double score = Math.log(v + 1) + 3.0 * l + 4.0 * col + 5.0 * com;
            p.setScore(round2(p.getScore() + score));
        }
        return new ArrayList<>(byDate.values());
    }

    private List<CreatorAnalyticsVO.TagInsight> buildTagInsights(List<Content> all) {
        if (all.isEmpty()) return List.of();
        List<Long> ids = all.stream().map(Content::getId).collect(Collectors.toList());
        List<ContentTag> rel = contentTagMapper.selectList(new LambdaQueryWrapper<ContentTag>().in(ContentTag::getContentId, ids));
        if (rel.isEmpty()) return List.of();
        Map<Long, Content> byContentId = all.stream().collect(Collectors.toMap(Content::getId, c -> c, (a, b) -> a));
        List<Long> tagIds = rel.stream().map(ContentTag::getTagId).distinct().collect(Collectors.toList());
        Map<Long, String> tagName = tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName, (a, b) -> a));

        Map<Long, CreatorAnalyticsVO.TagInsight> map = new LinkedHashMap<>();
        for (ContentTag ct : rel) {
            Content c = byContentId.get(ct.getContentId());
            if (c == null) continue;
            CreatorAnalyticsVO.TagInsight s = map.computeIfAbsent(ct.getTagId(), k -> {
                CreatorAnalyticsVO.TagInsight x = new CreatorAnalyticsVO.TagInsight();
                x.setTagId(k);
                x.setTagName(tagName.getOrDefault(k, "未命名标签"));
                x.setArticleCount(0);
                x.setViews(0L);
                x.setEngagement(0L);
                return x;
            });
            s.setArticleCount(s.getArticleCount() + 1);
            long v = c.getViewCount() == null ? 0 : c.getViewCount();
            long e = (c.getLikeCount() == null ? 0 : c.getLikeCount())
                    + (c.getCollectionCount() == null ? 0 : c.getCollectionCount())
                    + (c.getCommentCount() == null ? 0 : c.getCommentCount());
            s.setViews(s.getViews() + v);
            s.setEngagement(s.getEngagement() + e);
        }
        return map.values().stream()
                .sorted(Comparator.comparingLong(CreatorAnalyticsVO.TagInsight::getEngagement).reversed())
                .limit(8)
                .collect(Collectors.toList());
    }

    private List<CreatorAnalyticsVO.LengthBucket> buildLengthDistribution(List<Content> all) {
        int b0 = 0, b1 = 0, b2 = 0, b3 = 0;
        for (Content c : all) {
            int n = c.getBody() == null ? 0 : c.getBody().length();
            if (n < 500) b0++;
            else if (n < 1500) b1++;
            else if (n < 3000) b2++;
            else b3++;
        }
        int total = Math.max(all.size(), 1);
        List<CreatorAnalyticsVO.LengthBucket> list = new ArrayList<>();
        list.add(bucket("0-500", b0, total));
        list.add(bucket("500-1500", b1, total));
        list.add(bucket("1500-3000", b2, total));
        list.add(bucket("3000+", b3, total));
        return list;
    }

    private CreatorAnalyticsVO.LengthBucket bucket(String name, int count, int total) {
        CreatorAnalyticsVO.LengthBucket b = new CreatorAnalyticsVO.LengthBucket();
        b.setBucket(name);
        b.setCount(count);
        b.setRatio(round2((double) count / total));
        return b;
    }

    private List<CreatorAnalyticsVO.TopContent> buildTopContents(List<Content> all) {
        return all.stream()
                .filter(c -> STATUS_PUBLISHED.equalsIgnoreCase(c.getStatus()))
                .map(c -> {
                    long v = c.getViewCount() == null ? 0 : c.getViewCount();
                    long l = c.getLikeCount() == null ? 0 : c.getLikeCount();
                    long col = c.getCollectionCount() == null ? 0 : c.getCollectionCount();
                    long com = c.getCommentCount() == null ? 0 : c.getCommentCount();
                    double score = Math.log(v + 1) + 3.0 * l + 4.0 * col + 5.0 * com;
                    CreatorAnalyticsVO.TopContent t = new CreatorAnalyticsVO.TopContent();
                    t.setContentId(c.getId());
                    t.setTitle(c.getTitle() == null ? "未命名内容" : c.getTitle());
                    LocalDateTime p = c.getUpdatedAt() != null ? c.getUpdatedAt() : c.getCreatedAt();
                    t.setPublishedAt(p == null ? "" : p.format(ISO_FORMAT));
                    t.setViews(v);
                    t.setEngagement(l + col + com);
                    t.setScore(round2(score));
                    return t;
                })
                .sorted(Comparator.comparingDouble(CreatorAnalyticsVO.TopContent::getScore).reversed())
                .limit(8)
                .collect(Collectors.toList());
    }

    private CreatorAnalyticsVO.Heatmap buildHeatmap(List<Content> all) {
        int[] hours = new int[24];
        int[] weeks = new int[7];
        for (Content c : all) {
            LocalDateTime t = c.getCreatedAt();
            if (t == null) continue;
            hours[t.getHour()]++;
            DayOfWeek w = t.getDayOfWeek();
            weeks[w.getValue() - 1]++;
        }
        CreatorAnalyticsVO.Heatmap h = new CreatorAnalyticsVO.Heatmap();
        List<Integer> hourList = new ArrayList<>();
        for (int v : hours) hourList.add(v);
        List<Integer> weekList = new ArrayList<>();
        for (int v : weeks) weekList.add(v);
        h.setHourCounts(hourList);
        h.setWeekDayCounts(weekList);
        return h;
    }

    private Map<String, Object> fetchFollowStats(Long userId) {
        try {
            String url = interactionServiceUrl.replaceFirst("/$", "") + "/api/follow/me";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(userId));
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            return resp.getBody() == null ? Map.of() : resp.getBody();
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Long toLongSafe(Object x) {
        if (x instanceof Number n) return n.longValue();
        try {
            return x == null ? 0L : Long.parseLong(String.valueOf(x));
        } catch (Exception e) {
            return 0L;
        }
    }

    private Double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * 保存草稿：博客正文不能为空，知识库允许正文为空；标题为空则存为 [无标题]。标签按名称先查询，不存在则创建再关联，最多 5 个。
     */
    public SaveDraftResponse saveDraft(Long userId, SaveDraftRequest request) {
        String body = request.getBody() != null ? request.getBody().trim() : "";
        String title = request.getTitle() != null ? request.getTitle().trim() : "";
        if (title.isEmpty()) title = TITLE_EMPTY;

        Long requestId = request.getId();
        Content c;
        if (requestId != null) {
            c = contentMapper.selectById(requestId);
            boolean allowedType = TYPE_BLOG.equals(c.getType()) || TYPE_KNOWLEDGE.equals(c.getType());
            if (c == null || !userId.equals(c.getUserId()) || !allowedType) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或无权编辑");
            }
            // 博客草稿正文不能为空；知识库允许正文为空
            if (body.isEmpty() && TYPE_BLOG.equals(c.getType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "正文不能为空");
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
            if (TYPE_KNOWLEDGE.equals(c.getType())) {
                syncContentReferences(c.getId(), body);
                submitKnowledgeDocModerationIfPublic(c);
            }
            try {
                kbVectorService.refreshEmbeddingsForContent(c.getId());
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(ContentService.class).warn("保存草稿后刷新知识库向量失败 contentId={}", c.getId(), e);
            }
        } else {
            // 新建草稿：正文不能为空
            if (body.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "正文不能为空");
            }
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

    private void submitKnowledgeDocModerationIfPublic(Content c) {
        try {
            List<KnowledgeBaseContent> rel = knowledgeBaseContentMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeBaseContent>().eq(KnowledgeBaseContent::getContentId, c.getId()));
            if (rel == null || rel.isEmpty()) return;
            boolean inPublicKb = false;
            for (KnowledgeBaseContent r : rel) {
                KnowledgeBase kb = knowledgeBaseMapper.selectById(r.getKnowledgeBaseId());
                if (kb != null && KB_VISIBILITY_PUBLIC.equalsIgnoreCase(kb.getVisibility())) {
                    inPublicKb = true;
                    break;
                }
            }
            if (!inPublicKb) return;
            ModerationSubmitRequest req = new ModerationSubmitRequest();
            req.setResourceType("KNOWLEDGE_DOC");
            req.setResourceId(c.getId());
            req.setOwnerUserId(c.getUserId());
            req.setPayloadSnapshot(buildModerationPayload(c));
            moderationService.submitTask(req);
        } catch (Exception ignored) {
        }
    }

    /**
     * 双链笔记：解析正文中的 [[id:标题]] 或 [[标题]]，更新 content_reference 表。
     * 仅对知识库类型内容生效；[[标题]] 在当前笔记所属的任一知识库内按标题解析。
     */
    private void syncContentReferences(Long sourceContentId, String body) {
        if (body == null || body.isEmpty()) return;
        Set<Long> targetIds = new java.util.HashSet<>();
        Long anyKbId = knowledgeBaseContentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseContent>().eq(KnowledgeBaseContent::getContentId, sourceContentId))
                .stream()
                .map(KnowledgeBaseContent::getKnowledgeBaseId)
                .findFirst()
                .orElse(null);

        Matcher m = WIKILINK_PATTERN.matcher(body);
        while (m.find()) {
            String inner = m.group(1).trim();
            if (inner.isEmpty()) continue;
            Long targetId = null;
            if (inner.contains(":")) {
                int colon = inner.indexOf(':');
                String idPart = inner.substring(0, colon).trim();
                if (idPart.matches("\\d+")) {
                    targetId = Long.parseLong(idPart);
                    Content target = contentMapper.selectById(targetId);
                    if (target == null) targetId = null;
                }
            }
            if (targetId == null && anyKbId != null && !inner.contains(":")) {
                String titleToResolve = inner.trim();
                List<Long> contentIdsInKb = knowledgeBaseContentMapper.selectList(
                        new LambdaQueryWrapper<KnowledgeBaseContent>().eq(KnowledgeBaseContent::getKnowledgeBaseId, anyKbId))
                        .stream()
                        .map(KnowledgeBaseContent::getContentId)
                        .collect(Collectors.toList());
                if (!contentIdsInKb.isEmpty()) {
                    Content byTitle = contentMapper.selectOne(
                            new LambdaQueryWrapper<Content>()
                                    .eq(Content::getType, TYPE_KNOWLEDGE)
                                    .eq(Content::getTitle, titleToResolve)
                                    .in(Content::getId, contentIdsInKb)
                                    .last("LIMIT 1"));
                    if (byTitle != null) targetId = byTitle.getId();
                }
            }
            if (targetId != null && !targetId.equals(sourceContentId)) targetIds.add(targetId);
        }

        contentReferenceMapper.delete(new LambdaQueryWrapper<ContentReference>().eq(ContentReference::getSourceContentId, sourceContentId));
        LocalDateTime now = LocalDateTime.now();
        for (Long targetId : targetIds) {
            ContentReference ref = new ContentReference();
            ref.setSourceContentId(sourceContentId);
            ref.setTargetContentId(targetId);
            ref.setCreatedAt(now);
            contentReferenceMapper.insert(ref);
        }
    }

    /** 双链笔记：查询引用当前内容的笔记列表（反链/入链） */
    public List<ContentListItemVO> getBacklinks(Long contentId) {
        List<ContentReference> refs = contentReferenceMapper.selectList(
                new LambdaQueryWrapper<ContentReference>().eq(ContentReference::getTargetContentId, contentId));
        if (refs.isEmpty()) return Collections.emptyList();
        List<Long> sourceIds = refs.stream().map(ContentReference::getSourceContentId).distinct().collect(Collectors.toList());
        List<Content> contents = contentMapper.selectBatchIds(sourceIds);
        return contents.stream().map(this::toListItemVO).collect(Collectors.toList());
    }

    /** 双链笔记：查询当前内容引出的笔记列表（出链） */
    public List<ContentListItemVO> getOutlinks(Long contentId) {
        List<ContentReference> refs = contentReferenceMapper.selectList(
                new LambdaQueryWrapper<ContentReference>().eq(ContentReference::getSourceContentId, contentId));
        if (refs.isEmpty()) return Collections.emptyList();
        List<Long> targetIds = refs.stream().map(ContentReference::getTargetContentId).distinct().collect(Collectors.toList());
        List<Content> contents = contentMapper.selectBatchIds(targetIds);
        return contents.stream().map(this::toListItemVO).collect(Collectors.toList());
    }

    /** 双链：添加出链（当前内容 → 目标内容），需登录且当前内容归属当前用户 */
    @Transactional(rollbackFor = Exception.class)
    public void addOutlink(Long userId, Long sourceContentId, Long targetContentId) {
        if (sourceContentId == null || targetContentId == null || sourceContentId.equals(targetContentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数无效");
        }
        Content source = contentMapper.selectById(sourceContentId);
        if (source == null || !userId.equals(source.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或无权操作");
        }
        Content target = contentMapper.selectById(targetContentId);
        if (target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "目标内容不存在");
        }
        long exists = contentReferenceMapper.selectCount(
                new LambdaQueryWrapper<ContentReference>()
                        .eq(ContentReference::getSourceContentId, sourceContentId)
                        .eq(ContentReference::getTargetContentId, targetContentId));
        if (exists > 0) return;
        ContentReference ref = new ContentReference();
        ref.setSourceContentId(sourceContentId);
        ref.setTargetContentId(targetContentId);
        ref.setCreatedAt(LocalDateTime.now());
        contentReferenceMapper.insert(ref);
    }

    /** 双链：删除出链（当前内容 → 目标内容），需登录且当前内容归属当前用户 */
    public void deleteOutlink(Long userId, Long sourceContentId, Long targetContentId) {
        Content source = contentMapper.selectById(sourceContentId);
        if (source == null || !userId.equals(source.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或无权操作");
        }
        contentReferenceMapper.delete(
                new LambdaQueryWrapper<ContentReference>()
                        .eq(ContentReference::getSourceContentId, sourceContentId)
                        .eq(ContentReference::getTargetContentId, targetContentId));
    }

    /** 双链：添加入链（来源内容 → 当前内容），需登录且来源内容归属当前用户 */
    @Transactional(rollbackFor = Exception.class)
    public void addBacklink(Long userId, Long sourceContentId, Long targetContentId) {
        if (sourceContentId == null || targetContentId == null || sourceContentId.equals(targetContentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数无效");
        }
        Content source = contentMapper.selectById(sourceContentId);
        if (source == null || !userId.equals(source.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或无权操作");
        }
        Content target = contentMapper.selectById(targetContentId);
        if (target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "目标内容不存在");
        }
        long exists = contentReferenceMapper.selectCount(
                new LambdaQueryWrapper<ContentReference>()
                        .eq(ContentReference::getSourceContentId, sourceContentId)
                        .eq(ContentReference::getTargetContentId, targetContentId));
        if (exists > 0) return;
        ContentReference ref = new ContentReference();
        ref.setSourceContentId(sourceContentId);
        ref.setTargetContentId(targetContentId);
        ref.setCreatedAt(LocalDateTime.now());
        contentReferenceMapper.insert(ref);
    }

    /** 双链：删除入链（来源内容 → 当前内容），需登录且来源内容归属当前用户 */
    public void deleteBacklink(Long userId, Long sourceContentId, Long targetContentId) {
        Content source = contentMapper.selectById(sourceContentId);
        if (source == null || !userId.equals(source.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或无权操作");
        }
        contentReferenceMapper.delete(
                new LambdaQueryWrapper<ContentReference>()
                        .eq(ContentReference::getSourceContentId, sourceContentId)
                        .eq(ContentReference::getTargetContentId, targetContentId));
    }

    /** 删除与某内容相关的全部双链引用（该内容作为 source 或 target），用于从知识库移除文件时一并清理 */
    public void deleteAllReferencesForContent(Long contentId) {
        if (contentId == null) return;
        contentReferenceMapper.delete(
                new LambdaQueryWrapper<ContentReference>().eq(ContentReference::getSourceContentId, contentId));
        contentReferenceMapper.delete(
                new LambdaQueryWrapper<ContentReference>().eq(ContentReference::getTargetContentId, contentId));
    }

    /** 删除自己的博客/内容：仅本人可删，关联表由 DB CASCADE 清理 */
    public void deleteContent(Long userId, Long contentId) {
        if (userId == null || contentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数不能为空");
        }
        Content c = contentMapper.selectById(contentId);
        if (c == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在");
        }
        if (!userId.equals(c.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权删除该内容");
        }
        contentMapper.deleteById(contentId);
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
        // 与搜索/筛选一致：优先 updatedAt（发布时间），无则用 createdAt，便于列表与 ES 时间一致
        String publishedAtStr = c.getUpdatedAt() != null ? c.getUpdatedAt().format(ISO_FORMAT)
                : (c.getCreatedAt() != null ? c.getCreatedAt().format(ISO_FORMAT) : null);
        vo.setPublishedAt(publishedAtStr);
        return vo;
    }
}
