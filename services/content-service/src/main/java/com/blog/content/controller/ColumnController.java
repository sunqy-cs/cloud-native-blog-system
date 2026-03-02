package com.blog.content.controller;

import com.blog.content.dto.AddColumnContentRequest;
import com.blog.content.dto.ColumnVO;
import com.blog.content.dto.CreateColumnRequest;
import com.blog.content.dto.UpdateColumnRequest;
import com.blog.content.service.ColumnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/columns")
@RequiredArgsConstructor
public class ColumnController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final ColumnService columnService;

    @GetMapping("/me")
    public ResponseEntity<List<ColumnVO>> me(@RequestHeader(HEADER_USER_ID) Long userId) {
        List<ColumnVO> list = columnService.listMyColumns(userId);
        return ResponseEntity.ok(list);
    }

    /** 按专栏名称或描述模糊搜索（公开），用于搜索页「专栏」；无需认证 */
    @GetMapping("/search")
    public ResponseEntity<List<ColumnVO>> search(@RequestParam(required = false) String q) {
        List<ColumnVO> list = columnService.searchByName(q);
        return ResponseEntity.ok(list);
    }

    /** 按用户 ID 获取专栏列表（公开），用于他人博客页顶栏「全部 / 专栏」导航；无需认证 */
    @GetMapping("/list")
    public ResponseEntity<List<ColumnVO>> list(@RequestParam(required = false) Long userId) {
        if (userId == null) return ResponseEntity.ok(List.of());
        List<ColumnVO> list = columnService.listColumnsByUserId(userId);
        return ResponseEntity.ok(list);
    }

    /** 按 ID 获取专栏详情（公开），用于专栏详情页 */
    @GetMapping("/{id}")
    public ResponseEntity<ColumnVO> getById(@PathVariable Long id) {
        ColumnVO vo = columnService.getById(id);
        return ResponseEntity.ok(vo);
    }

    @PostMapping("/{id}/contents")
    public ResponseEntity<Void> addContent(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestBody AddColumnContentRequest request) {
        if (request == null || request.getContentId() == null) {
            return ResponseEntity.badRequest().build();
        }
        columnService.addContentToColumn(userId, id, request.getContentId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/contents")
    public ResponseEntity<Void> removeContent(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestParam Long contentId) {
        columnService.removeContentFromColumn(userId, id, contentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<ColumnVO> create(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestBody CreateColumnRequest request) {
        ColumnVO vo = columnService.createColumn(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(vo);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ColumnVO> update(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id,
            @RequestBody UpdateColumnRequest request) {
        ColumnVO vo = columnService.updateColumn(userId, id, request);
        return ResponseEntity.ok(vo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        columnService.deleteColumn(userId, id);
        return ResponseEntity.noContent().build();
    }
}
