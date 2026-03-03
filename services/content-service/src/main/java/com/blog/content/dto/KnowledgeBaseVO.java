package com.blog.content.dto;

import lombok.Data;

@Data
public class KnowledgeBaseVO {
    private Long id;
    private String name;
    private String cover;
    private String description;
    private String visibility;
    private Long ownerId;
    private String ownerName;
    private String ownerAvatar;
    private Integer subCount;
    private Integer contentCount;
    private String createdAt;
    private String updatedAt;
    /** 当前用户是否已订阅（仅 getById 时可能返回） */
    private Boolean subscribed;
}
