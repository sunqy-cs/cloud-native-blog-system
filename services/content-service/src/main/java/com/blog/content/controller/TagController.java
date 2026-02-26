package com.blog.content.controller;

import com.blog.content.dto.TagVO;
import com.blog.content.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 获取全部主标签（12 个），用于创作页必选其一。
     */
    @GetMapping("/main")
    public ResponseEntity<List<TagVO>> main() {
        return ResponseEntity.ok(tagService.listMainTags());
    }
}
