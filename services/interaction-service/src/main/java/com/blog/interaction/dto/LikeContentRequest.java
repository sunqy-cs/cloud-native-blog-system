package com.blog.interaction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LikeContentRequest {
    @NotNull(message = "contentId 不能为空")
    private Long contentId;
}
