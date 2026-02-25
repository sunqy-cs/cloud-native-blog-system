package com.blog.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_collection")
public class ContentCollection {

    private Long userId;
    private Long contentId;
    private Long folderId;
    private LocalDateTime createdAt;
}
