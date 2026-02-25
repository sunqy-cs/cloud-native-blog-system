package com.blog.interaction.dto;

import lombok.Data;

import java.util.List;

@Data
public class ContentLikesMeResponse {
    private List<ContentLikeItemVO> list;
    private Long total;
}
