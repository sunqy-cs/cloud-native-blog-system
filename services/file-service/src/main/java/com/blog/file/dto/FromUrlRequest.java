package com.blog.file.dto;

import lombok.Data;

@Data
public class FromUrlRequest {
    private String url;
    private String prefix;
}
