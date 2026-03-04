package com.blog.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_reference")
public class ContentReference {

    private Long sourceContentId;
    private Long targetContentId;
    private LocalDateTime createdAt;
}
