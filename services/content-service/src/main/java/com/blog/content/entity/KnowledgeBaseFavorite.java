package com.blog.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_base_favorite")
public class KnowledgeBaseFavorite {

    private Long userId;
    private Long knowledgeBaseId;
    private LocalDateTime createdAt;
}
