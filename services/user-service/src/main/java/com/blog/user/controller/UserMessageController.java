package com.blog.user.controller;

import com.blog.user.dto.UserMessagePageResponse;
import com.blog.user.service.UserMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me/messages")
@RequiredArgsConstructor
public class UserMessageController {

    private static final String HEADER_USER_ID = "X-User-Id";
    private final UserMessageService userMessageService;

    @GetMapping
    public ResponseEntity<UserMessagePageResponse> list(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return ResponseEntity.ok(userMessageService.list(userId, page, pageSize, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@RequestHeader(HEADER_USER_ID) Long userId) {
        return ResponseEntity.ok(Map.of("count", userMessageService.unreadCount(userId)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@RequestHeader(HEADER_USER_ID) Long userId, @PathVariable Long id) {
        userMessageService.markRead(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@RequestHeader(HEADER_USER_ID) Long userId) {
        userMessageService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }
}
