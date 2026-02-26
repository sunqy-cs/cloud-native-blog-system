package com.blog.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("follow")
public class Follow {

    private Long followerId;
    private Long followeeId;
    private LocalDateTime createdAt;
}
