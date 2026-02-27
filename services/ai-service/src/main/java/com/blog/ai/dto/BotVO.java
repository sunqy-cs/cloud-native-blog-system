package com.blog.ai.dto;

import lombok.Data;

@Data
public class BotVO {
    private Long id;
    private String name;
    private String avatar;
    private String style;
    private Long mainTagId;
    private String mainTagName;
    private String summaryStyle;
    private String wordCountPreference;
    private String createdAt;
    private String updatedAt;
}
