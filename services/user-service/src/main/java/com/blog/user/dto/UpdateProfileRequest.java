package com.blog.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String nickname;
    private String avatar;
    private String cover;
    private String gender;
    private String intro;
    private String residence;
    private String industry;
    private String bio;
    private String wechatId;
}
