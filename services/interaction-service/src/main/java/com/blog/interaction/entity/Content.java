package com.blog.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 仅用于 interaction-service 内读取 content 表（校验作者、评论管理左侧列表） */
@Data
@TableName("content")
public class Content {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private Integer commentCount;
    private LocalDateTime updatedAt;
}
