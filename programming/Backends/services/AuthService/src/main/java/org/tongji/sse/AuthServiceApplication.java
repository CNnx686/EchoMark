package org.tongji.sse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * AuthServiceApplication 是认证服务的主应用程序类。
 * 负责启动 Spring Boot 应用程序。
 */
@SpringBootApplication
public class AuthServiceApplication {

    /**
     * 应用程序的入口点。
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    /**
     * 密码加密 Bean
     * @return PasswordEncoder 实例，用于加密用户密码
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}