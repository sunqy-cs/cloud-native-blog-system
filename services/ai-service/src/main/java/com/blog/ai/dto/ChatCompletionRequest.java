package com.blog.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatCompletionRequest {
    private String model;
    private List<ChatMessage> messages;
    @JsonProperty("stream")
    private Boolean stream;
}
