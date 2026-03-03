package com.blog.content.dto;

import lombok.Data;

@Data
public class CreateKnowledgeBaseFileRequest {
    /** 初始标题，不传则使用「未命名」 */
    private String title;
}
