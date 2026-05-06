package org.tongji.sse.authservicetest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.tongji.sse.controller.AuthController;
import org.tongji.sse.dto.ApiResponse;
import org.tongji.sse.dto.LoginResponseDTO;
import org.tongji.sse.dto.UserLoginRequestDTO;
import org.tongji.sse.service.AuthService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)  // 关闭安全过滤器
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testLogin_Success() throws Exception {
        UserLoginRequestDTO loginDTO = new UserLoginRequestDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("123456");

        LoginResponseDTO loginResponse = LoginResponseDTO.builder()
                .username("testuser")
                .token("fake-jwt-token")
                .build();

        Mockito.when(authService.login(Mockito.any(UserLoginRequestDTO.class)))
                .thenReturn(ApiResponse.success(loginResponse));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value(""))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.token").value("fake-jwt-token"));
    }
}
