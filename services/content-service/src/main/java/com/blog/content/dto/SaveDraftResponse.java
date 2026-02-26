package com.blog.content.dto;

import lombok.Data;

@Data
public class SaveDraftResponse {
    private Long id;
    private String title;
    private String status;
    private String createdAt;
}
