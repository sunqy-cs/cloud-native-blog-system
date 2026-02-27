package com.blog.content.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublishResponse {
    private Long id;
    private String title;
    private String status;
    private String publishedAt;
}
