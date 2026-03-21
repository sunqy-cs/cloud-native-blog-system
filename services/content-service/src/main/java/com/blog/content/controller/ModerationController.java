package com.blog.content.controller;

import com.blog.content.dto.ModerationHumanReviewRequest;
import com.blog.content.dto.ModerationStatsResponse;
import com.blog.content.dto.ModerationSubmitRequest;
import com.blog.content.dto.ModerationTaskPageResponse;
import com.blog.content.dto.ModerationTaskVO;
import com.blog.content.entity.ModerationTask;
import com.blog.content.service.ModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private static final String HEADER_USER_ID = "X-User-Id";
    private final ModerationService moderationService;

    /** 业务服务提交审核任务（content / interaction / user 都可调用） */
    @PostMapping("/tasks/submit")
    public ResponseEntity<Map<String, Object>> submitTask(@RequestBody ModerationSubmitRequest req) {
        ModerationTask task = moderationService.submitTask(req);
        if (task == null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("skipped", true, "reason", "admin_exempt"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", task.getId(),
                "status", task.getStatus(),
                "aiDecision", task.getAiDecision()
        ));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ModerationTaskPageResponse> listTasks(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean finishedOnly
    ) {
        return ResponseEntity.ok(moderationService.listTasks(userId, page, pageSize, resourceType, status, finishedOnly));
    }

    @GetMapping("/stats")
    public ResponseEntity<ModerationStatsResponse> stats(@RequestHeader(HEADER_USER_ID) Long userId) {
        return ResponseEntity.ok(moderationService.stats(userId));
    }

    @PostMapping("/tasks/{id}/human-review")
    public ResponseEntity<ModerationTaskVO> humanReview(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestBody ModerationHumanReviewRequest req) {
        return ResponseEntity.ok(moderationService.humanReview(userId, id, req));
    }

    @PostMapping("/tasks/{id}/ai-review")
    public ResponseEntity<ModerationTaskVO> aiReview(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(moderationService.rerunAiReview(userId, id));
    }
}
