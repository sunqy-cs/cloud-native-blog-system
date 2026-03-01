package com.blog.interaction.dto;

import lombok.Data;

@Data
public class CommentVO {
    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private Long contentId;
    private String body;
    private Long parentId;
    private Boolean isHot;
    private String createdAt;
    private Boolean isAuthor;
}
