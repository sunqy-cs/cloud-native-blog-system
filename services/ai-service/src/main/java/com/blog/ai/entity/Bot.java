package com.blog.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bot")
public class Bot {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String avatar;
    private String style;
    private Long mainTagId;
    private String summaryStyle;
    private String wordCountPreference;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
