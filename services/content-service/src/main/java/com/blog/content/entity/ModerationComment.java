package com.blog.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("comment")
public class ModerationComment {
    private Long id;
    private Long userId;
    private Long contentId;
    private String body;
    private String moderationStatus;
}
