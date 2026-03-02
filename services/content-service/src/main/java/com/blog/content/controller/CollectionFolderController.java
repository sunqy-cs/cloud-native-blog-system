package com.blog.content.controller;

import com.blog.content.dto.AddFolderContentRequest;
import com.blog.content.dto.CollectionFolderVO;
import com.blog.content.dto.ContentsMeResponse;
import com.blog.content.dto.CreateFolderRequest;
import com.blog.content.dto.UpdateFolderRequest;
import com.blog.content.service.CollectionFolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collection-folders")
@RequiredArgsConstructor
public class CollectionFolderController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final CollectionFolderService collectionFolderService;

    @GetMapping("/me")
    public ResponseEntity<List<CollectionFolderVO>> me(@RequestHeader(HEADER_USER_ID) Long userId) {
        List<CollectionFolderVO> list = collectionFolderService.listMyFolders(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionFolderVO> getById(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        CollectionFolderVO vo = collectionFolderService.getFolder(userId, id);
        return ResponseEntity.ok(vo);
    }

    @GetMapping("/containing/{contentId}")
    public ResponseEntity<List<Long>> getFolderIdsContaining(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long contentId) {
        List<Long> folderIds = collectionFolderService.getFolderIdsContainingContent(userId, contentId);
        return ResponseEntity.ok(folderIds);
    }

    @GetMapping("/{id}/contents")
    public ResponseEntity<ContentsMeResponse> listContents(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;
        ContentsMeResponse res = collectionFolderService.listContentsInFolder(userId, id, page, pageSize);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/contents")
    public ResponseEntity<Void> addContent(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestBody AddFolderContentRequest request) {
        if (request == null || request.getContentId() == null) {
            return ResponseEntity.badRequest().build();
        }
        collectionFolderService.addContentToFolder(userId, id, request.getContentId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/contents")
    public ResponseEntity<Void> removeContent(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestParam Long contentId) {
        collectionFolderService.removeContentFromFolder(userId, id, contentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<CollectionFolderVO> create(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestBody CreateFolderRequest request) {
        CollectionFolderVO vo = collectionFolderService.createFolder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(vo);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CollectionFolderVO> update(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestBody UpdateFolderRequest request) {
        CollectionFolderVO vo = collectionFolderService.updateFolder(userId, id, request);
        return ResponseEntity.ok(vo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        collectionFolderService.deleteFolder(userId, id);
        return ResponseEntity.noContent().build();
    }
}
