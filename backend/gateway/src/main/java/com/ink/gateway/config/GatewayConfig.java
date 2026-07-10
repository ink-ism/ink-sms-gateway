package com.ink.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

/**
 * 网关配置类
 */
@Configuration
public class GatewayConfig {

    /**
     * IP限流Key解析器
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }

    /**
     * 全局跨域过滤器
     */
    @Bean
    public WebFilter corsFilter() {
        return (exchange, chain) -> {
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Headers", "*");
            exchange.getResponse().getHeaders().add("Access-Control-Max-Age", "3600");
            
            if ("OPTIONS".equals(exchange.getRequest().getMethod().name())) {
                exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.OK);
                return Mono.empty();
            }
            
            return chain.filter(exchange);
        };
    }
}