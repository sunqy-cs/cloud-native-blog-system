package com.blog.content.dto;

import lombok.Data;

@Data
public class ModerationStatsResponse {
    private long pending;
    private long pendingHuman;
    private long todayFinished;
    private long rejected7d;
}
