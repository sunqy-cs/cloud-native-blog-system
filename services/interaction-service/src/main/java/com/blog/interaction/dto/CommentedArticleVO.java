package com.blog.interaction.dto;

import lombok.Data;

@Data
public class CommentedArticleVO {
    private Long contentId;
    private String title;
    private Integer commentCount;
    private String lastCommentAt;
}
