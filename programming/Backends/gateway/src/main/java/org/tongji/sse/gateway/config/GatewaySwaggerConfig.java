package org.tongji.sse.gateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewaySwaggerConfig {

    @Bean
    public GroupedOpenApi sseApi() {
        return GroupedOpenApi.builder()
                .group("SSE 推送服务")
                .pathsToMatch("/api/sse/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户服务 (User)")
                .pathsToMatch("/api/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("认证服务 (Auth)")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi socialApi() {
        return GroupedOpenApi.builder()
                .group("社交服务 (Social)")
                .pathsToMatch("/api/social/**")
                .build();
    }

    @Bean
    public GroupedOpenApi audioApi() {
        return GroupedOpenApi.builder()
                .group("音频服务 (Audio)")
                .pathsToMatch("/api/audio/**")
                .build();
    }

    @Bean
    public GroupedOpenApi notifyApi() {
        return GroupedOpenApi.builder()
                .group("通知服务 (Notify)")
                .pathsToMatch("/api/notifications/**")
                .build();
    }
}