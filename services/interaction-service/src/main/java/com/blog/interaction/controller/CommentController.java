package com.blog.interaction.controller;

import com.blog.interaction.dto.CommentVO;
import com.blog.interaction.dto.CommentedArticleVO;
import com.blog.interaction.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final CommentService commentService;

    @GetMapping("/commented-articles")
    public ResponseEntity<List<CommentedArticleVO>> listCommentedArticles(
            @RequestHeader(HEADER_USER_ID) Long userId) {
        List<CommentedArticleVO> list = commentService.listCommentedArticles(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/list")
    public ResponseEntity<List<CommentVO>> listByContentId(
            @RequestParam Long contentId,
            @RequestHeader(HEADER_USER_ID) Long userId) {
        List<CommentVO> list = commentService.listByContentId(contentId, userId);
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/hot")
    public ResponseEntity<Void> setHot(
            @PathVariable Long id,
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestBody Map<String, Boolean> body) {
        Boolean hot = body != null ? body.get("hot") : null;
        commentService.setHot(id, userId, Boolean.TRUE.equals(hot));
        return ResponseEntity.noContent().build();
    }
}
