package org.tongji.sse.authservicetest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.tongji.sse.dto.*;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate; // Spring 注入，自动配置

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/auth";
    }

    @Test
    void testRegisterLoginAndGetUsername() {
        String email = "integrationUser@example.com";

        // ----------------- 发送验证码 -----------------
        RegisterCodeRequestDTO codeRequestDTO = new RegisterCodeRequestDTO();
        codeRequestDTO.setEmail(email);

        ResponseEntity<ApiResponse<Void>> codeResponse = restTemplate.exchange(
                baseUrl() + "/register/code",
                HttpMethod.POST,
                new HttpEntity<>(codeRequestDTO),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertEquals(HttpStatus.OK, codeResponse.getStatusCode());
        assertNotNull(codeResponse.getBody());
        assertEquals(200, codeResponse.getBody().getCode());

        // 从 Redis 获取验证码
        String code = redisTemplate.opsForValue().get("REGISTER_CODE:" + email);
        assertNotNull(code, "Verification code should be in Redis");

        // ----------------- 注册 -----------------
        UserRegisterRequestDTO registerDTO = new UserRegisterRequestDTO();
        registerDTO.setUsername("integrationUser");
        registerDTO.setPassword("123456");
        registerDTO.setEmail(email);
        registerDTO.setCode(code);

        ResponseEntity<ApiResponse<Void>> registerResponse = restTemplate.exchange(
                baseUrl() + "/register",
                HttpMethod.POST,
                new HttpEntity<>(registerDTO),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody());
        assertEquals(200, registerResponse.getBody().getCode());

        // ----------------- 登录 -----------------
        UserLoginRequestDTO loginDTO = new UserLoginRequestDTO();
        loginDTO.setUsername("integrationUser");
        loginDTO.setPassword("123456");

        ResponseEntity<ApiResponse<LoginResponseDTO>> loginResponse = restTemplate.exchange(
                baseUrl() + "/login",
                HttpMethod.POST,
                new HttpEntity<>(loginDTO),
                new ParameterizedTypeReference<ApiResponse<LoginResponseDTO>>() {}
        );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertEquals(200, loginResponse.getBody().getCode());

        String token = loginResponse.getBody().getData().getToken();
        assertNotNull(token);
        assertEquals("integrationUser", loginResponse.getBody().getData().getUsername());

        // ----------------- 获取用户名 -----------------
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<UserResponseDTO>> userInfoResponse = restTemplate.exchange(
                baseUrl() + "/me",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ApiResponse<UserResponseDTO>>() {}
        );

        assertEquals(HttpStatus.OK, userInfoResponse.getStatusCode());
        assertNotNull(userInfoResponse.getBody());
        assertEquals(200, userInfoResponse.getBody().getCode());
        assertEquals("integrationUser", userInfoResponse.getBody().getData().getUsername());
    }
}
