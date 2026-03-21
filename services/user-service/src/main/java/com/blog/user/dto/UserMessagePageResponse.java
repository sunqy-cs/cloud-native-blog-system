package com.blog.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserMessagePageResponse {
    private List<UserMessageVO> records;
    private long total;
}
