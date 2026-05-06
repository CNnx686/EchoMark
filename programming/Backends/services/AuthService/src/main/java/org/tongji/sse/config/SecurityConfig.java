package org.tongji.sse.config;

// Spring Security配置相关类
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// 自定义JWT过滤器
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.tongji.sse.security.JwtAuthenticationFilter;

/**
 * SecurityConfig 类负责配置应用程序的安全设置。
 * 这是Spring Security的核心配置类。
 */
@Configuration
public class SecurityConfig {

    // JWT认证过滤器
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 构造函数，注入JwtAuthenticationFilter
     * @param jwtAuthenticationFilter JWT认证过滤器
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 配置安全过滤器链 - Spring Security的核心配置
     * @param http HttpSecurity对象，用于配置HTTP请求的安全性
     * @return 配置后的SecurityFilterChain
     * @throws Exception 配置过程中可能抛出的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（跨站请求伪造）保护，因为REST API通常不需要
                .csrf(AbstractHttpConfigurer::disable) // 关闭 CSRF
                // 设置无状态会话，因为使用JWT不需要session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 无状态
                // 配置请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 放行 Swagger UI 相关资源
                        .requestMatchers(
                                "/swagger-ui/**",      // Swagger UI页面
                                "/swagger-ui.html",    // Swagger HTML页面
                                "/v3/api-docs/**",     // API文档JSON
                                "/swagger-resources/**", // Swagger资源
                                "/webjars/**"          // Webjar资源
                        ).permitAll() // 允许匿名访问
                        // 放行登录和注册接口
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/register/code",
                                "/api/auth/login",
                                "/api/auth/password/**"
                        ).permitAll()
                        .anyRequest().authenticated() // 其他接口需要认证
                )
                // 添加JWT过滤器，在UsernamePasswordAuthenticationFilter之前执行
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置认证管理器
     * @param authConfig AuthenticationConfiguration对象
     * @return 配置后的AuthenticationManager
     * @throws Exception 配置过程中可能抛出的异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}