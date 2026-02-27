package com.blog.content.dto;

import lombok.Data;

@Data
public class CreateBlogBotRequest {
    private String name;
    private String avatar;
    private String style;
    private Long mainTagId;
    private String summaryStyle;
    private String wordCountPreference;
}
