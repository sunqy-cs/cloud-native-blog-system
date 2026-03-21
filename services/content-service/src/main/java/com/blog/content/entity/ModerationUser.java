package com.blog.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user")
public class ModerationUser {
    private Long id;
    private String username;
    private String phone;
    private String nickname;
    private String avatar;
    private String cover;
    private String gender;
    private String intro;
    private String residence;
    private String industry;
    private String bio;
    private String role;
    private String profileModerationStatus;
}
