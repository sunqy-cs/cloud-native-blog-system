package com.blog.content.dto;

import lombok.Data;

@Data
public class ColumnVO {
    private Long id;
    /** 专栏所属用户 ID，仅搜索接口返回，用于前端跳转博客页 */
    private Long userId;
    private String name;
    private String description;
    private String cover;
    private Integer articleCount;
    private String createdAt;
    private String updatedAt;
}
