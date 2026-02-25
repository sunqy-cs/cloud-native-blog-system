package com.blog.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_like")
public class ContentLike {

    private Long userId;
    private Long contentId;
    private LocalDateTime createdAt;
}
