package com.blog.ai.dto;

import lombok.Data;

@Data
public class CreateBotRequest {
    private String name;
    private String avatar;
    private String style;
    private Long mainTagId;
    private String summaryStyle;
    private String wordCountPreference;
}
