package com.blog.content.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreatorAnalyticsVO {
    private Overview overview;
    private List<TrendPoint> trend;
    private List<TagInsight> tagInsights;
    private List<LengthBucket> lengthDistribution;
    private List<TopContent> topContents;
    private Heatmap heatmap;

    @Data
    public static class Overview {
        private Long totalContents;
        private Long publishedContents;
        private Long draftContents;
        private Long totalViews;
        private Long totalLikes;
        private Long totalCollections;
        private Long totalComments;
        private Long totalEngagement;
        private Double avgViewsPerPublished;
        private Double avgEngagementPerPublished;
        private Double publishRate;
        private Long followers;
        private Long following;
    }

    @Data
    public static class TrendPoint {
        private String date;
        private Integer publishedCount;
        private Long views;
        private Long likes;
        private Long collections;
        private Long comments;
        private Double score;
    }

    @Data
    public static class TagInsight {
        private Long tagId;
        private String tagName;
        private Integer articleCount;
        private Long views;
        private Long engagement;
    }

    @Data
    public static class LengthBucket {
        private String bucket;
        private Integer count;
        private Double ratio;
    }

    @Data
    public static class TopContent {
        private Long contentId;
        private String title;
        private String publishedAt;
        private Long views;
        private Long engagement;
        private Double score;
    }

    @Data
    public static class Heatmap {
        private List<Integer> hourCounts;
        private List<Integer> weekDayCounts;
    }
}
