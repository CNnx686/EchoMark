package org.tongji.sse.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 全局请求日志过滤器
 * 用于监控流经 Gateway 的所有请求
 */
@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 获取请求路径
        String path = exchange.getRequest().getURI().getPath();
        // 2. 获取请求方法
        String method = exchange.getRequest().getMethod().name();
        // 3. 获取客户端 IP (简单获取)
        String host = Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getHostString();

        log.info("Gateway 请求拦截: Method: {}, Host: {}, Path: {}", method, host, path);

        // 继续执行过滤链
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // 请求处理完成后，打印状态码
            log.info("Gateway 请求响应: Path: {}, Status: {}", path, exchange.getResponse().getStatusCode());
        }));
    }

    @Override
    public int getOrder() {
        // 设置优先级，越小越先执行
        return 0;
    }
}
