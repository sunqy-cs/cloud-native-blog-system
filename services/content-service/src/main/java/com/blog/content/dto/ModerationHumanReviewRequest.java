package com.blog.content.dto;

import lombok.Data;

@Data
public class ModerationHumanReviewRequest {
    /** APPROVE / REJECT */
    private String decision;
    private String note;
}
