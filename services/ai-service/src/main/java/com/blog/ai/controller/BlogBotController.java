package com.blog.ai.controller;

import com.blog.ai.dto.BotVO;
import com.blog.ai.dto.CreateBotRequest;
import com.blog.ai.service.BotService;
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

    private final BotService botService;

    @GetMapping("/me")
    public ResponseEntity<List<BotVO>> me(@RequestHeader(HEADER_USER_ID) Long userId) {
        List<BotVO> list = botService.listMyBots(userId);
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<BotVO> create(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestBody CreateBotRequest request) {
        BotVO vo = botService.createBot(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(vo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @PathVariable Long id) {
        botService.deleteBot(userId, id);
        return ResponseEntity.noContent().build();
    }
}
