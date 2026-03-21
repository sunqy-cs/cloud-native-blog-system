package com.blog.content.dto;

import lombok.Data;

@Data
public class ModerationSubmitRequest {
    private String resourceType;
    private Long resourceId;
    private Long ownerUserId;
    private String payloadSnapshot;
}
