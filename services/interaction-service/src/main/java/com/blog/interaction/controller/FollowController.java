package com.blog.interaction.controller;

import com.blog.interaction.dto.FollowStatsVO;
import com.blog.interaction.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final FollowService followService;

    @GetMapping("/me")
    public ResponseEntity<FollowStatsVO> me(@RequestHeader(HEADER_USER_ID) Long userId) {
        FollowStatsVO stats = followService.getFollowStats(userId);
        return ResponseEntity.ok(stats);
    }
}
