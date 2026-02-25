package com.blog.interaction.dto;

import lombok.Data;

@Data
public class ContentLikeItemVO {
    private Long contentId;
    /** 点赞时间，格式 yyyy-MM-dd HH:mm */
    private String likedAt;
}
