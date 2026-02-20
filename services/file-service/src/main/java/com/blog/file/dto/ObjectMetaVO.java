package com.blog.file.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectMetaVO {

    private String key;
    private String url;
    private long size;
    private String contentType;
}
