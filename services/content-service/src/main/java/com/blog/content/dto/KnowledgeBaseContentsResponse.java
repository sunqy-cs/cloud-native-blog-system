package com.blog.content.dto;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeBaseContentsResponse {
    private List<KnowledgeBaseContentItemVO> list;
    private Long total;
}
