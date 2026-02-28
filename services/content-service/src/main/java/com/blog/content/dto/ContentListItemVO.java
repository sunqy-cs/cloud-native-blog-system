package com.blog.content.dto;

import lombok.Data;

import java.util.List;

@Data
public class ContentListItemVO {
    private Long id;
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
    /** 标签名称列表；仅在有搜索关键词 q 时填充，供前端高亮 */
    private List<String> tagNames;
}
