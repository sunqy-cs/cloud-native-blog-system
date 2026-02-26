package com.blog.content.dto;

import lombok.Data;

@Data
public class ContentMeStatsVO {
    /** 总阅读量：当前用户所有内容 view_count 之和 */
    private Long totalViewCount;
    /** 总点赞量：当前用户所有内容 like_count 之和 */
    private Long totalLikeCount;
    /** 收藏数：当前用户所有内容 collection_count 之和 */
    private Long totalCollectionCount;
    /** 昨日阅读增长（无按日日志时固定 0） */
    private Long yesterdayViewDelta;
    /** 昨日点赞增长 */
    private Long yesterdayLikeDelta;
    /** 昨日收藏增长 */
    private Long yesterdayCollectionDelta;
}
