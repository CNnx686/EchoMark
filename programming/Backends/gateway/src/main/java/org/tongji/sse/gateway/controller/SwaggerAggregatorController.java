package org.tongji.sse.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/swagger")
public class SwaggerAggregatorController {

    @GetMapping("/swagger-config")
    public Map<String, Object> swaggerConfig() {
        Map<String, Object> config = new HashMap<>();
        List<Map<String, String>> urls = List.of(
                Map.of("name", "SSE 推送服务", "url", "/api/sse/v3/api-docs"),
                Map.of("name", "用户服务 (User)", "url", "/api/users/v3/api-docs"),
                Map.of("name", "社交服务 (Social)", "url", "/api/social/v3/api-docs"),
                Map.of("name", "认证服务 (Auth)", "url", "/api/auth/v3/api-docs"),
                Map.of("name", "通知服务 (Notify)", "url", "/api/notifications/v3/api-docs"),
                Map.of("name", "音频服务 (Audio)", "url", "/api/audio/v3/api-docs")
        );
        config.put("urls", urls);
        config.put("urls.primaryName", null);   // 关键
        config.put("configUrl", "/swagger/swagger-config");
        config.put("oauth2RedirectUrl", "http://localhost:8080/webjars/swagger-ui/oauth2-redirect.html");
        config.put("validatorUrl", "");
        return config;
    }
}
