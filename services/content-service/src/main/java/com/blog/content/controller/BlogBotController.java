package com.blog.content.controller;

import com.blog.content.dto.BlogBotVO;
import com.blog.content.dto.CreateBlogBotRequest;
import com.blog.content.service.BlogBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blog-bots")
@RequiredArgsConstructor
public class BlogBotController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final BlogBotService blogBotService;

    @GetMapping("/me")
    public ResponseEntity<List<BlogBotVO>> me(@RequestHeader(HEADER_USER_ID) Long userId) {
        List<BlogBotVO> list = blogBotService.listMyBots(userId);
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<BlogBotVO> create(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestBody CreateBlogBotRequest request) {
        BlogBotVO vo = blogBotService.createBot(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(vo);
    }
}
