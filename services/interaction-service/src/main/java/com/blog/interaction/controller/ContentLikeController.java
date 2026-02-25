package com.blog.interaction.controller;

import com.blog.interaction.dto.ContentLikesMeResponse;
import com.blog.interaction.service.ContentLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
