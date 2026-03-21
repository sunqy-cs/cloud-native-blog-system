package com.blog.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserMessageVO {
    private Long id;
    private Long senderUserId;
    private String title;
    private String body;
    private String msgType;
    private String scene;
    private String extra;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
