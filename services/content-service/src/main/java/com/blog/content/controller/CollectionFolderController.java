package com.blog.content.controller;

import com.blog.content.dto.CollectionFolderVO;
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
