package org.tongji.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.tongji.sse.component.AudioTranscoder;
import org.tongji.sse.dto.UploadAudioRequest;
import org.tongji.sse.service.AudioService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AudioServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AudioControllerResilienceTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Mock AudioService，避免真实 ffmpeg / 文件系统依赖
     * 同时用于制造“失败”，触发熔断
     */
    @MockBean
    private AudioService audioService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private AudioTranscoder audioTranscoder;

    /**
     * =========================
     * 测试 1：限流是否生效
     * =========================
     *
     * application-test.properties 中配置：
     *
     * resilience4j.ratelimiter.instances.audioApiRateLimiter.limit-for-period=2
     * resilience4j.ratelimiter.instances.audioApiRateLimiter.limit-refresh-period=10s
     */
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void should_trigger_rate_limiter_when_exceeding_limit() throws Exception {

        // mock 正常返回（避免失败影响熔断）
        when(audioService.upload(any(), any(), any()))
                .thenReturn(null);

        for (int i = 0; i < 3; i++) {
            ResultActions result = mockMvc.perform(
                    multipart("/api/audio/upload")
                            .file("file", "fake-audio".getBytes())
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header("Authorization", "Bearer test-token")
            );

            if (i < 2) {
                // 前两次成功
                result.andExpect(status().isOk());
            } else {
                // 第三次触发限流
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(429))
                        .andExpect(jsonPath("$.msg").exists());
            }
        }
    }

    /**
     * =========================
     * 测试 2：熔断是否打开
     * =========================
     *
     * application-test.properties 中配置：
     *
     * sliding-window-size=2
     * minimum-number-of-calls=2
     * failure-rate-threshold=50
     */
    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    void should_open_circuit_breaker_after_failures() throws Exception {

        // 强制 service 抛异常，制造失败
        when(audioService.upload(any(), any(), any()))
                .thenThrow(new RuntimeException("mock upload failure"));

        for (int i = 0; i < 3; i++) {
            ResultActions result = mockMvc.perform(
                    multipart("/api/audio/upload")
                            .file("file", "fake-audio".getBytes())
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header("Authorization", "Bearer test-token")
            );

            if (i < 2) {
                // 前两次是真实失败（异常进入熔断统计）
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(500))
                        .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("mock upload failure")));

            } else {
                // 第三次：熔断打开，直接拒绝
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(503))
                        .andExpect(jsonPath("$.msg").exists());
            }
        }
    }
}
