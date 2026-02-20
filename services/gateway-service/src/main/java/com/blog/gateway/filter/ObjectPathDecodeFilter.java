package com.blog.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 将 /api/objects/ 路径中的 %2F 解码为 / 再转发，避免旧内容里带编码斜杠的图片 URL 被下游拒掉返回 400。
 */
@Component
@Order(-2)
public class ObjectPathDecodeFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith("/api/objects/") && !path.equals("/api/objects")) {
            return chain.filter(exchange);
        }
        String decoded = path.replace("%2F", "/").replace("%2f", "/");
        if (decoded.equals(path)) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest().mutate().path(decoded).build();
        return chain.filter(exchange.mutate().request(request).build());
    }
}
