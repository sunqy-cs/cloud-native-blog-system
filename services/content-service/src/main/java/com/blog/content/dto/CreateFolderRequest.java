package com.blog.content.dto;

import lombok.Data;

@Data
public class CreateFolderRequest {
    private String name;
    private String description;
}
