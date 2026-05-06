package org.tongji.sse.llmservicetest.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.tongji.sse.controller.LlmController;
import org.tongji.sse.dto.RecommendationResponse;
import org.tongji.sse.service.LlmService;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LlmController.class)
@AutoConfigureMockMvc(addFilters = false)
class LlmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LlmService llmService;

    @Test
    void testGetRecommendation_Success() throws Exception {
        RecommendationResponse response = RecommendationResponse.builder()
                .reason("Because you like jazz")
                .audioIds(Arrays.asList(1L, 2L, 3L))
                .build();

        Mockito.when(llmService.getRecommendation(Mockito.any(HttpServletRequest.class), Mockito.anyString()))
                .thenReturn(response);

        mockMvc.perform(get("/api/llm/recommendation")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.reason").value("Because you like jazz"))
                .andExpect(jsonPath("$.data.audioIds[0]").value(1))
                .andExpect(jsonPath("$.data.audioIds[1]").value(2))
                .andExpect(jsonPath("$.data.audioIds[2]").value(3));
    }
}
