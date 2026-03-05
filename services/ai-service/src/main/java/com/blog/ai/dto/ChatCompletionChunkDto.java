package com.blog.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/** 流式响应单条 data 的 JSON 结构（OpenAI/DeepSeek 兼容） */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionChunkDto {
    private List<Choice> choices;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Delta delta;
        private Integer index;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        private String content;
    }
}
