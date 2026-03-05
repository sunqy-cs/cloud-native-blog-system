package com.blog.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_conversation")
public class RagConversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long kbId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
