package com.blog.dto;

import lombok.Data;

import java.util.List;

@Data
public class IndexRequestDto {
    private Long id;
    private Long userId;
    private String title;
    private String summary;
    private String body;
    private List<String> tagNames;
    private String publishedAt;
}
