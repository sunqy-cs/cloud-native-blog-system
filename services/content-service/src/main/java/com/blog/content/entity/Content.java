package com.blog.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content")
public class Content {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private Long columnId;
    private String title;
    private String summary;
    private String body;
    private String cover;
    private String status;
    private String articleType;
    private String creationStatement;
    private String visibility;
    private Integer likeCount;
    private Integer collectionCount;
    private Integer viewCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
