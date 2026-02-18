package com.blog.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String role;
    private LocalDateTime createdAt;
}
