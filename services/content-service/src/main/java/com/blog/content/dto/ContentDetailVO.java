package com.blog.content.dto;

import lombok.Data;

import java.util.List;

/** 编辑用内容详情（含正文、标签名列表等） */
@Data
public class ContentDetailVO {
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
}
