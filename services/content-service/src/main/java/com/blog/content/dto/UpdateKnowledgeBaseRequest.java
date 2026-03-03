package com.blog.content.dto;

import lombok.Data;

@Data
public class UpdateKnowledgeBaseRequest {
    private String name;
    private String description;
    private String cover;
    private String visibility;
}
