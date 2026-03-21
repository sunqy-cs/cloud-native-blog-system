package com.blog.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String cover;
    private String gender;
    private String intro;
    private String residence;
    private String industry;
    private String bio;
    /** 脱敏展示，如 138****8000 */
    private String phone;
    private String role;
    private LocalDateTime createdAt;
}
