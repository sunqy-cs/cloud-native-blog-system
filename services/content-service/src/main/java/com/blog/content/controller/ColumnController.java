package com.blog.content.controller;

import com.blog.content.dto.ColumnVO;
import com.blog.content.dto.CreateColumnRequest;
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

    @PostMapping
    public ResponseEntity<ColumnVO> create(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestBody CreateColumnRequest request) {
        ColumnVO vo = columnService.createColumn(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(vo);
    }
}
