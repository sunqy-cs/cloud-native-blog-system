package com.blog.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckLikedResponse {
    /** 当前用户是否已点赞该内容 */
    private boolean liked;
}
