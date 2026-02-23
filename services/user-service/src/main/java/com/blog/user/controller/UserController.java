package com.blog.user.controller;

import com.blog.user.dto.RegisterRequest;
import com.blog.user.dto.UpdateProfileRequest;
import com.blog.user.dto.UserVO;
import com.blog.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserVO> create(@Valid @RequestBody RegisterRequest req) {
        UserVO vo = userService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(vo);
    }

    @GetMapping("/me")
    public ResponseEntity<UserVO> me(@RequestHeader(HEADER_USER_ID) Long userId) {
        UserVO vo = userService.getById(userId);
        if (vo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(vo);
    }

    @PatchMapping("/me")
    public ResponseEntity<UserVO> updateMe(
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestBody UpdateProfileRequest req) {
        UserVO vo = userService.updateProfile(userId, req);
        if (vo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(vo);
    }
}
