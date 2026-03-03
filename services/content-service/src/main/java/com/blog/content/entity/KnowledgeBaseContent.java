package com.blog.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_base_content")
public class KnowledgeBaseContent {

    private Long knowledgeBaseId;
    private Long contentId;
    private LocalDateTime createdAt;
}
