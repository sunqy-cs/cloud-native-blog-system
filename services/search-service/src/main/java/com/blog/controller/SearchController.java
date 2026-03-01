package com.blog.controller;

import com.blog.dto.IndexRequestDto;
import com.blog.service.ContentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ContentSearchService contentSearchService;

    /** content-service 发布文章后调用，建/更新索引 */
    @PostMapping("/index")
    public ResponseEntity<Void> index(@RequestBody IndexRequestDto dto) {
        contentSearchService.index(dto);
        return ResponseEntity.ok().build();
    }

    /** 综合搜索：标题、摘要、正文、标签。支持 sort（comprehensive/newest/likes）、time（all/1d/1w/1m/3m/6m/1y）。 */
    @GetMapping
    public ResponseEntity<Map<String, List<Long>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String time) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(Map.of("ids", List.<Long>of()));
        }
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;
        List<Long> ids = contentSearchService.search(q.trim(), page, pageSize, sort, time);
        return ResponseEntity.ok(Map.of("ids", ids));
    }
}
