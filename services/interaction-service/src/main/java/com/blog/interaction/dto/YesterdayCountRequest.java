package com.blog.interaction.dto;

import lombok.Data;

import java.util.List;

@Data
public class YesterdayCountRequest {
    /** 内容 ID 列表，统计这些内容在昨日新增的点赞数 */
    private List<Long> contentIds;
}
