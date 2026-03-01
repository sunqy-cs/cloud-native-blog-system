package com.blog.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Document(indexName = "blog_contents")
public class ContentDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String summary;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String body;

    @Field(type = FieldType.Keyword)
    private List<String> tagNames;

    /** 发布时间，格式 yyyy-MM-dd 或 yyyy-MM-dd'T'HH:mm:ss，存为 Keyword 避免 ES 日期反序列化问题 */
    @Field(type = FieldType.Keyword)
    private String publishedAt;
}
