package com.blog.content.dto;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeBaseListResponse {
    private List<KnowledgeBaseVO> list;
    private Long total;
}
