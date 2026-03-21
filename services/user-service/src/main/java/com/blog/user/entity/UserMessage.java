package com.blog.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_message")
public class UserMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long senderUserId;
    private String title;
    private String body;
    private String msgType;
    private String scene;
    private String extra;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
