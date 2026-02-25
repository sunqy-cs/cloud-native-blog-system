package com.blog.content.dto;

import lombok.Data;

@Data
public class ContentListItemVO {
    private Long id;
    private String title;
    private String summary;
    private String cover;
    private Integer viewCount;
    private Integer likeCount;
    private Integer collectionCount;
    private String createdAt;
}
