package com.blog.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_base")
public class KnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String cover;
    private String description;
    private String visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
