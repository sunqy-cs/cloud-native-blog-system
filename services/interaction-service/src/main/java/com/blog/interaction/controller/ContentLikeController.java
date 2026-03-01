package com.blog.interaction.controller;

import com.blog.interaction.dto.CheckLikedResponse;
import com.blog.interaction.dto.ContentLikesMeResponse;
import com.blog.interaction.dto.LikeContentRequest;
import com.blog.interaction.dto.YesterdayCountRequest;
import com.blog.interaction.dto.YesterdayCountResponse;
import com.blog.interaction.service.ContentLikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content-likes")
@RequiredArgsConstructor
public class ContentLikeController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final ContentLikeService contentLikeService;

    @GetMapping("/me")
    public ResponseEntity<ContentLikesMeResponse> me(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;
        ContentLikesMeResponse res = contentLikeService.listMyLiked(userId, page, pageSize);
        return ResponseEntity.ok(res);
    }

    /** 查询当前用户是否已点赞指定内容（未登录时返回 liked=false） */
    @GetMapping("/check")
    public ResponseEntity<CheckLikedResponse> check(
            @RequestHeader(value = HEADER_USER_ID, required = false) Long userId,
            @RequestParam Long contentId) {
        boolean liked = contentLikeService.hasLiked(userId, contentId);
        return ResponseEntity.ok(new CheckLikedResponse(liked));
    }

    /** 点赞文章（需登录） */
    @PostMapping
    public ResponseEntity<Void> like(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @Valid @RequestBody LikeContentRequest request) {
        contentLikeService.like(userId, request.getContentId());
        return ResponseEntity.noContent().build();
    }

    /** 取消点赞文章（需登录） */
    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> unlike(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long contentId) {
        contentLikeService.unlike(userId, contentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 统计指定内容在昨日新增的点赞数，供创作者中心汇总「昨日点赞增长」调用。
     */
    @PostMapping("/yesterday-count")
    public ResponseEntity<YesterdayCountResponse> yesterdayCount(@RequestBody YesterdayCountRequest request) {
        List<Long> ids = request != null ? request.getContentIds() : null;
        long count = contentLikeService.countYesterdayByContentIds(ids);
        return ResponseEntity.ok(new YesterdayCountResponse(count));
    }
}
