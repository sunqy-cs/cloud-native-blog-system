package com.blog.content.dto;

import lombok.Data;

import java.util.List;

@Data
public class ModerationTaskPageResponse {
    private List<ModerationTaskVO> records;
    private long total;
}
