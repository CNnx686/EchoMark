package org.tongji.sse.userservicetest.controller;

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
import org.tongji.sse.controller.UserController; // 导入被测试的控制器
import org.tongji.sse.dto.ApiResponse; // 导入API响应类
import org.tongji.sse.dto.UserProfileDTO; // 导入用户个人资料DTO类
import org.tongji.sse.security.JwtUtil; // 导入JWT工具类
import org.tongji.sse.service.UserService; // 导入用户服务接口

/**
 * 用户控制器测试类
 * 使用MockMvc测试UserController的HTTP端点
 */
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户控制器测试类
 * 使用MockMvc测试UserController的HTTP端点
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc; // 用于模拟HTTP请求

    @MockBean
    private UserService userService; // 模拟用户服务

    @SuppressWarnings("unused")
    private ObjectMapper objectMapper; // 用于JSON处理

    /**
     * 测试前的初始化方法
     * 创建ObjectMapper实例
     */
    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
    }

    /**
     * 测试获取指定用户个人资料的端点
     * 验证返回的状态码和响应内容
     */
    @SuppressWarnings("null")
    @Test
    void testGetUserProfile_Success() throws Exception {
        UserProfileDTO profileDTO = UserProfileDTO.builder()
                .userId(1L)
                .nickname("testuser")
                .bio("A bio")
                .build();

        Mockito.when(userService.getProfile(1L))
                .thenReturn(ApiResponse.success(profileDTO));

        mockMvc.perform(get("/api/users/1/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").value("testuser"));
    }

    /**
     * 测试获取当前登录用户个人资料的端点
     * 验证返回的状态码和响应内容
     */
    @SuppressWarnings("null")
    @Test
    void testGetMyProfile_Success() throws Exception {
        UserProfileDTO profileDTO = UserProfileDTO.builder()
                .userId(1L)
                .nickname("testuser")
                .bio("A bio")
                .build();

        String token = JwtUtil.generateToken(1L);

        Mockito.when(userService.getMyProfile(Mockito.any()))
                .thenReturn(ApiResponse.success(profileDTO));

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").value("testuser"));
    }
}
