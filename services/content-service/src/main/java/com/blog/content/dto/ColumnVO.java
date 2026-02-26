package com.blog.content.dto;

import lombok.Data;

@Data
public class ColumnVO {
    private Long id;
    private String name;
    private String description;
    private String cover;
    private Integer articleCount;
    private String createdAt;
    private String updatedAt;
}
