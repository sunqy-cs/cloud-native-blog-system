package com.blog.user.controller;

import com.blog.user.dto.LoginRequest;
import com.blog.user.dto.LoginResponse;
import com.blog.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<LoginResponse> create(@Valid @RequestBody LoginRequest req) {
        LoginResponse res = userService.login(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @DeleteMapping("/current")
    public ResponseEntity<Void> delete() {
        return ResponseEntity.noContent().build();
    }
}
