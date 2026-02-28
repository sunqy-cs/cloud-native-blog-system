package com.blog.content.controller;

import com.blog.content.dto.ContentDetailVO;
import com.blog.content.dto.ContentListItemVO;
import com.blog.content.dto.ContentMeStatsVO;
import com.blog.content.dto.ContentsMeResponse;
import com.blog.content.dto.PublishResponse;
import com.blog.content.dto.SaveDraftRequest;
import com.blog.content.dto.SaveDraftResponse;
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

    @GetMapping("/me")
    public ResponseEntity<ContentsMeResponse> me(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String q) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;
        ContentsMeResponse res = contentService.listMyContents(userId, page, pageSize, visibility, status, sortBy, order, q);
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

    @GetMapping("/{id}")
    public ResponseEntity<ContentDetailVO> getForEdit(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        ContentDetailVO vo = contentService.getForEdit(userId, id);
        return ResponseEntity.ok(vo);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<PublishResponse> publish(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        PublishResponse res = contentService.publish(userId, id);
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
