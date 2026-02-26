package com.blog.content.dto;

import lombok.Data;

import java.util.List;

@Data
public class SaveDraftRequest {
    private String title;
    private String body;
    private String summary;
    private String cover;
    private Long columnId;
    private String articleType;
    private String creationStatement;
    private String visibility;
    /** 标签名称列表；保存时按名称查询，不存在则创建（is_main=0），再建立 content_tag 关联。最多 5 个。 */
    private List<String> tagNames;
}
