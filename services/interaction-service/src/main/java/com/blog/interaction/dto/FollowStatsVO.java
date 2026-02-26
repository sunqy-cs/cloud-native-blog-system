package com.blog.interaction.dto;

import lombok.Data;

@Data
public class FollowStatsVO {
    private Long followingCount;
    private Long followerCount;
    /** 昨日新增粉丝数（昨日 0 点至今日 0 点） */
    private Long yesterdayFollowerDelta;
}
