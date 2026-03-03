package com.blog.content.controller;

import com.blog.content.dto.KnowledgeBaseListResponse;
import com.blog.content.dto.*;
import com.blog.content.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping("/me")
    public ResponseEntity<List<KnowledgeBaseVO>> me(@RequestHeader(HEADER_USER_ID) Long userId) {
        List<KnowledgeBaseVO> list = knowledgeBaseService.listMy(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/popular")
    public ResponseEntity<KnowledgeBaseListResponse> popular(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String q) {
        KnowledgeBaseListResponse res = knowledgeBaseService.listPopular(userId, page, pageSize, q);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/subscribed")
    public ResponseEntity<List<KnowledgeBaseVO>> subscribed(@RequestHeader(HEADER_USER_ID) Long userId) {
        List<KnowledgeBaseVO> list = knowledgeBaseService.listSubscribed(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBaseVO> getById(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        KnowledgeBaseVO vo = knowledgeBaseService.getById(userId, id);
        return ResponseEntity.ok(vo);
    }

    @GetMapping("/{id}/contents")
    public ResponseEntity<KnowledgeBaseContentsResponse> listContents(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;
        KnowledgeBaseContentsResponse res = knowledgeBaseService.listContents(userId, id, page, pageSize);
        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<KnowledgeBaseVO> create(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestBody CreateKnowledgeBaseRequest request) {
        KnowledgeBaseVO vo = knowledgeBaseService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(vo);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<KnowledgeBaseVO> update(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestBody UpdateKnowledgeBaseRequest request) {
        KnowledgeBaseVO vo = knowledgeBaseService.update(userId, id, request);
        return ResponseEntity.ok(vo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        knowledgeBaseService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/contents")
    public ResponseEntity<Void> addContent(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestBody AddKnowledgeBaseContentRequest request) {
        if (request == null || request.getContentId() == null) {
            return ResponseEntity.badRequest().build();
        }
        knowledgeBaseService.addContent(userId, id, request.getContentId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** 在知识库中新建文件（草稿），请求体可传 title，不传则「未命名」 */
    @PostMapping("/{id}/contents/new-file")
    public ResponseEntity<KnowledgeBaseContentItemVO> createNewFile(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestBody(required = false) CreateKnowledgeBaseFileRequest request) {
        KnowledgeBaseContentItemVO vo = knowledgeBaseService.createNewFile(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(vo);
    }

    @DeleteMapping("/{id}/contents")
    public ResponseEntity<Void> removeContent(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestParam Long contentId) {
        knowledgeBaseService.removeContent(userId, id, contentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/subscribe")
    public ResponseEntity<Void> subscribe(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        knowledgeBaseService.subscribe(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/subscribe")
    public ResponseEntity<Void> unsubscribe(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        knowledgeBaseService.unsubscribe(userId, id);
        return ResponseEntity.noContent().build();
    }
}
