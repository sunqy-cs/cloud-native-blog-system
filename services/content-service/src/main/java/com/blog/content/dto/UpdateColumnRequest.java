package com.blog.content.dto;

import lombok.Data;

@Data
public class UpdateColumnRequest {
    private String name;
    private String description;
    private String cover;
}
