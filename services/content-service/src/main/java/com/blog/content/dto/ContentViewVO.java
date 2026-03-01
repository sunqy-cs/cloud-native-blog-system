package com.blog.content.dto;

import lombok.Data;

import java.util.List;

/** 文章公开阅读用（已发布博客），含阅读数、点赞数、作者 id */
@Data
public class ContentViewVO {
    private Long id;
    private String title;
    private String body;
    private String summary;
    private String cover;
    private Long columnId;
    private String articleType;
    private String creationStatement;
    private String visibility;
    private List<String> tagNames;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private String createdAt;
    private Long userId;
}
