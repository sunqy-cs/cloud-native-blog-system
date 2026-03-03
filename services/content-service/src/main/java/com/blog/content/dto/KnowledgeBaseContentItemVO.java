package com.blog.content.dto;

import lombok.Data;

@Data
public class KnowledgeBaseContentItemVO {
    private Long id;
    private String title;
    private String summary;
    private String cover;
    /** 内容类型：BLOG-博客 / KNOWLEDGE-知识库文件 */
    private String type;
    /** 作者用户 ID，用于判断是否可编辑（重命名/跳转编辑） */
    private Long userId;
}
