package com.blog.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "gateway.auth")
public class GatewayAuthProperties {

    /** 无需 JWT 的请求：Method:Path，如 POST:/api/sessions */
    private List<String> permitAll = new ArrayList<>();
}
