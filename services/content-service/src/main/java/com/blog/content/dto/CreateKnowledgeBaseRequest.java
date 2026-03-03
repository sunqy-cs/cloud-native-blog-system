package com.blog.content.dto;

import lombok.Data;

@Data
public class CreateKnowledgeBaseRequest {
    private String name;
    private String description;
    private String cover;
    private String visibility;
}
