package com.blog.content.dto;

import lombok.Data;

@Data
public class CollectionFolderVO {
    private Long id;
    private String name;
    private String description;
    private Boolean isDefault;
    private Integer count;
    private String createdAt;
}
