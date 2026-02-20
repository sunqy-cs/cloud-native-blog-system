package com.blog.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.blog.gateway.config.GatewayAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 网关统一鉴权：对非白名单的 /api/** 校验 JWT，通过则向下游传递 X-User-Id。
 */
@Component
@Order(-1)
@RequiredArgsConstructor
public class JwtAuthGlobalFilter implements org.springframework.cloud.gateway.filter.GlobalFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String API_PREFIX = "/api/";

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final GatewayAuthProperties gatewayAuth;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod() != null ? request.getMethod().name() : "";

        if (!path.startsWith(API_PREFIX)) {
            return chain.filter(exchange);
        }

        // 白名单：POST /api/sessions、POST /api/users 无需 Token
        if (isPermitAll(method, path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange.getResponse(), "未认证或 Token 失效");
        }
        String token = authHeader.substring(7);
        Long userId = verifyAndGetUserId(token);
        if (userId == null) {
            return unauthorized(exchange.getResponse(), "未认证或 Token 失效");
        }

        ServerHttpRequest mutated = request.mutate()
                .header(HEADER_USER_ID, String.valueOf(userId))
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isPermitAll(String method, String path) {
        List<String> permitAll = gatewayAuth.getPermitAll();
        if (permitAll == null) return false;
        for (String entry : permitAll) {
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) continue;
            if (!method.equalsIgnoreCase(parts[0].trim())) continue;
            String pathPattern = parts[1].trim();
            if (pathPattern.equals(path)) return true;
            if (pathPattern.endsWith("/**") && path.startsWith(pathPattern.substring(0, pathPattern.length() - 3)))
                return true;
        }
        return false;
    }

    private Long verifyAndGetUserId(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(token);
            return Long.parseLong(jwt.getSubject());
        } catch (JWTVerificationException | NumberFormatException e) {
            return null;
        }
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        byte[] body;
        try {
            body = objectMapper.writeValueAsString(Map.of("message", message, "code", "UNAUTHORIZED"))
                    .getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            body = "{\"message\":\"未认证或 Token 失效\",\"code\":\"UNAUTHORIZED\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
