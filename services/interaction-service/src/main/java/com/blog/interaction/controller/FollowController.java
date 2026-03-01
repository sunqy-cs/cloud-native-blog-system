package com.blog.interaction.controller;

import com.blog.interaction.dto.FollowStatsVO;
import com.blog.interaction.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    /** 校验当前用户是否已关注指定用户（供 content-service 做 visibility=FANS 可见性判断）；未登录返回 false */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> check(
            @RequestHeader(value = HEADER_USER_ID, required = false) Long followerId,
            @RequestParam Long followeeId) {
        boolean following = followService.isFollowing(followerId, followeeId);
        return ResponseEntity.ok(Map.of("following", following));
    }

    /** 指定用户的关注统计（公开），用于他人个人主页展示；无需认证 */
    @GetMapping("/stats")
    public ResponseEntity<FollowStatsVO> stats(@RequestParam Long userId) {
        FollowStatsVO vo = followService.getFollowStatsByUserId(userId);
        return ResponseEntity.ok(vo);
    }

    /** 关注指定用户（需登录，不能关注自己） */
    @PostMapping
    public ResponseEntity<Void> follow(
            @RequestHeader(HEADER_USER_ID) Long currentUserId,
            @RequestBody Map<String, Long> body) {
        Long followeeId = body != null ? body.get("followeeId") : null;
        if (followeeId == null) {
            return ResponseEntity.badRequest().build();
        }
        followService.follow(currentUserId, followeeId);
        return ResponseEntity.noContent().build();
    }

    /** 取消关注指定用户 */
    @DeleteMapping("/{followeeId}")
    public ResponseEntity<Void> unfollow(
            @RequestHeader(HEADER_USER_ID) Long currentUserId,
            @PathVariable Long followeeId) {
        followService.unfollow(currentUserId, followeeId);
        return ResponseEntity.noContent().build();
    }
}
