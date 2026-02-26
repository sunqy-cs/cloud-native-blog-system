package com.blog.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("content_tag")
public class ContentTag {
    private Long contentId;
    private Long tagId;
}
