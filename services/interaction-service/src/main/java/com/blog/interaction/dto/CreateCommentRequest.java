package com.blog.interaction.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class CreateCommentRequest {

    @NotNull(message = "contentId 不能为空")
    private Long contentId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容最多 500 字")
    private String body;

    private Long parentId;
}
