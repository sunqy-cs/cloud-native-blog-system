package com.blog.content.dto;

import lombok.Data;

import java.util.List;

@Data
public class ContentListItemVO {
    private Long id;
    /** 作者用户 ID，用于关注流等展示作者昵称/头像 */
    private Long userId;
    private String title;
    private String summary;
    private String cover;
    private String status;       // DRAFT / PUBLISHED
    private String articleType;  // ORIGINAL / REPRINT / TRANSLATED
    private Integer viewCount;
    private Integer likeCount;
    private Integer collectionCount;
    private Integer commentCount;
    private String createdAt;
    /** 发布时间（updatedAt），与搜索/筛选用的时间一致；列表展示建议用此字段 */
    private String publishedAt;
    /** 标签名称列表；仅在有搜索关键词 q 时填充，供前端高亮 */
    private List<String> tagNames;
    /** 热榜热度分（仅热榜接口返回），engagement * time_decay */
    private Double hotScore;
}
