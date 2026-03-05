package com.blog.content.controller;

import com.blog.content.dto.ContentDetailVO;
import com.blog.content.dto.ContentListItemVO;
import com.blog.content.dto.ContentMeStatsVO;
import com.blog.content.dto.ContentsMeResponse;
import com.blog.content.dto.ContentViewVO;
import com.blog.content.dto.PublishResponse;
import com.blog.content.dto.SaveDraftRequest;
import com.blog.content.dto.SaveDraftResponse;
import com.blog.content.dto.UpdateContentTitleRequest;
import com.blog.content.dto.AddContentReferenceRequest;
import com.blog.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final ContentService contentService;

    /** 我的内容列表：内容管理仅展示博客，不展示知识库文件（type 不传或 BLOG 时只查 BLOG） */
    @GetMapping("/me")
    public ResponseEntity<ContentsMeResponse> me(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long columnId,
            @RequestParam(required = false) String type) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;
        ContentsMeResponse res = contentService.listMyContents(userId, page, pageSize, visibility, status, sortBy, order, q, columnId, type);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/me/stats")
    public ResponseEntity<ContentMeStatsVO> meStats(@RequestHeader(HEADER_USER_ID) Long userId) {
        ContentMeStatsVO stats = contentService.getStats(userId);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/draft")
    public ResponseEntity<SaveDraftResponse> saveDraft(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestBody SaveDraftRequest request) {
        SaveDraftResponse res = contentService.saveDraft(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /** 全量重建综合搜索索引（把已发布文章同步到 search-service），调用一次即可。 */
    @PostMapping("/reindex-search")
    public ResponseEntity<java.util.Map<String, Integer>> reindexSearch() {
        int indexed = contentService.reindexAllPublishedForSearch();
        return ResponseEntity.ok(java.util.Map.of("indexed", indexed));
    }

    /** 热榜：按 engagement * time_decay 排序，engagement = 1*log(阅读+1)+3*点赞+5*收藏+8*评论，time_decay = 1/(1+小时/12) */
    @GetMapping("/hot")
    public ResponseEntity<ContentsMeResponse> hot(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 50;
        ContentsMeResponse res = contentService.listHot(page, pageSize);
        return ResponseEntity.ok(res);
    }

    /** 公开阅读：已发布博客，无需登录；路径 /api/contents/view/{id} 便于网关放行。传 X-User-Id 时按用户去重，仅首次阅读增加阅读量 */
    @GetMapping("/view/{id}")
    public ResponseEntity<ContentViewVO> getForView(
            @PathVariable Long id,
            @RequestHeader(value = HEADER_USER_ID, required = false) Long userId) {
        ContentViewVO vo = contentService.getForView(id, userId);
        return ResponseEntity.ok(vo);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentDetailVO> getForEdit(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        ContentDetailVO vo = contentService.getForEdit(userId, id);
        return ResponseEntity.ok(vo);
    }

    /** 删除自己的博客/内容（仅本人可删） */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        contentService.deleteContent(userId, id);
        return ResponseEntity.noContent().build();
    }

    /** 双链笔记：引用该内容的笔记列表（入链/反链） */
    @GetMapping("/{id}/backlinks")
    public ResponseEntity<List<ContentListItemVO>> getBacklinks(@PathVariable Long id) {
        List<ContentListItemVO> list = contentService.getBacklinks(id);
        return ResponseEntity.ok(list);
    }

    /** 双链笔记：当前内容引出的笔记列表（出链） */
    @GetMapping("/{id}/outlinks")
    public ResponseEntity<List<ContentListItemVO>> getOutlinks(@PathVariable Long id) {
        List<ContentListItemVO> list = contentService.getOutlinks(id);
        return ResponseEntity.ok(list);
    }

    /** 双链：添加出链（当前内容 → 目标内容），需认证 */
    @PostMapping("/{id}/references")
    public ResponseEntity<Void> addOutlink(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestBody AddContentReferenceRequest request) {
        Long targetId = request != null ? request.getTargetId() : null;
        if (targetId == null) {
            return ResponseEntity.badRequest().build();
        }
        contentService.addOutlink(userId, id, targetId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** 双链：删除出链，需认证 */
    @DeleteMapping("/{id}/references/{targetId}")
    public ResponseEntity<Void> deleteOutlink(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @PathVariable Long targetId) {
        contentService.deleteOutlink(userId, id, targetId);
        return ResponseEntity.ok().build();
    }

    /** 双链：添加入链（来源内容 → 当前内容），需认证 */
    @PostMapping("/{id}/backlinks")
    public ResponseEntity<Void> addBacklink(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestBody AddContentReferenceRequest request) {
        Long sourceId = request != null ? request.getSourceId() : null;
        if (sourceId == null) {
            return ResponseEntity.badRequest().build();
        }
        contentService.addBacklink(userId, sourceId, id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** 双链：删除入链，需认证 */
    @DeleteMapping("/{id}/backlinks/{sourceId}")
    public ResponseEntity<Void> deleteBacklink(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @PathVariable Long sourceId) {
        contentService.deleteBacklink(userId, sourceId, id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateTitle(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestBody UpdateContentTitleRequest request) {
        String title = request != null ? request.getTitle() : null;
        contentService.updateTitle(userId, id, title);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<PublishResponse> publish(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        PublishResponse res = contentService.publish(userId, id);
        return ResponseEntity.ok(res);
    }

    /** 公开推荐列表：已发布博客，可选 mainTagId、userId（按用户筛即「TA的博客」）、userIds（多用户即「关注流」）；传 X-User-Id 时按 visibility 过滤他人博客 */
    @GetMapping("/list")
    public ResponseEntity<ContentsMeResponse> list(
            @RequestParam(required = false) Long mainTagId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) List<Long> userIds,
            @RequestParam(required = false) Long columnId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order,
            @RequestHeader(value = HEADER_USER_ID, required = false) Long currentUserId) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;
        ContentsMeResponse res = contentService.listPublic(mainTagId, userId, userIds, columnId, page, pageSize, sortBy, order, currentUserId);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/by-ids")
    public ResponseEntity<List<ContentListItemVO>> byIds(@RequestParam String ids) {
        if (ids == null || ids.isBlank()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .distinct()
                .collect(Collectors.toList());
        List<ContentListItemVO> list = contentService.listByIds(idList);
        return ResponseEntity.ok(list);
    }
}
