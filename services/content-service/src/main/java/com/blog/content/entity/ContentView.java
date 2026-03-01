package com.blog.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 阅读记录表：同一用户对同一文章只计一次阅读 */
@Data
@TableName("content_view")
public class ContentView {

    private Long userId;
    private Long contentId;
    private LocalDateTime createdAt;
}
