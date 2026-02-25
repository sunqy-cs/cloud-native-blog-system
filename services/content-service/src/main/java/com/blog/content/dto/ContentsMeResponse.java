package com.blog.content.dto;

import lombok.Data;

import java.util.List;

@Data
public class ContentsMeResponse {
    private List<ContentListItemVO> list;
    private Long total;
}
