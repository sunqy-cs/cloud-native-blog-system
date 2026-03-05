package com.blog.content.dto;

import lombok.Data;

@Data
public class AddContentReferenceRequest {
    /** 出链：目标内容 ID；入链时不用 */
    private Long targetId;
    /** 入链：引用当前内容的来源内容 ID；出链时不用 */
    private Long sourceId;
}
