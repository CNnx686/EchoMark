package org.tongji.sse.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tongji.sse.dto.LlmRequest;
import org.tongji.sse.dto.LlmResponse;
import org.tongji.sse.dto.RecommendationResponse;

public interface LlmService {
    LlmResponse generateResponse(LlmRequest request);
    void generateResponseStream(LlmRequest request, SseEmitter emitter);
    RecommendationResponse getRecommendation(HttpServletRequest request, String language);
}
