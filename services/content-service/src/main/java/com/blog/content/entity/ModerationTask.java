package com.blog.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("moderation_task")
public class ModerationTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String resourceType;
    private Long resourceId;
    private Long ownerUserId;
    private String status;
    private String aiDecision;
    private String aiDetail;
    private LocalDateTime aiReviewedAt;
    private Long humanReviewerId;
    private String humanDecision;
    private String humanNote;
    private LocalDateTime humanReviewedAt;
    private String payloadSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
