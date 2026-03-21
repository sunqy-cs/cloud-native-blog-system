package com.blog.content.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ModerationTaskVO {
    private Long id;
    private String resourceType;
    private Long resourceId;
    private String resourceTitle;
    private Long ownerUserId;
    private String ownerUsername;
    private String ownerAvatar;
    private String status;
    private String aiDecision;
    private String aiDetail;
    private LocalDateTime aiReviewedAt;
    private Long humanReviewerId;
    private String humanReviewerName;
    private String humanDecision;
    private String humanNote;
    private LocalDateTime humanReviewedAt;
    private String payloadSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
