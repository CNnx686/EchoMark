package org.tongji.sse.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tongji.sse.dto.ApiResponse;
import org.tongji.sse.dto.LlmRequest;
import org.tongji.sse.dto.LlmResponse;
import org.tongji.sse.dto.RecommendationResponse;
import org.tongji.sse.service.LlmService;

@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final LlmService llmService;

    public LlmController(LlmService llmService) {
        this.llmService = llmService;
    }

    /**
     * 生成LLM响应
     * @param request LlmRequest对象，包含用户的提示信息
     * @return LlmResponse对象，包含LLM生成的响应内容
     */

    @PostMapping("/generate")
    public ApiResponse<LlmResponse> generate(@RequestBody LlmRequest request) {
        LlmResponse response = llmService.generateResponse(request);
        return ApiResponse.success(response);
    }

    /**
     * 生成LLM流式响应
     * @param request LlmRequest对象
     * @return SseEmitter用于流式传输
     */
    @PostMapping(value = "/generateStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public SseEmitter generateStream(@RequestBody LlmRequest request) {
        SseEmitter emitter = new SseEmitter(600000L); // 10 min 超时
        llmService.generateResponseStream(request, emitter);
        return emitter;
    }

    /**
     * 获取智能推荐
     * @param request HTTP请求
     * @return 推荐结果
     */
    @GetMapping("/recommendation")
    public ApiResponse<RecommendationResponse> getRecommendation(HttpServletRequest request,
                                                                 @RequestParam(required = false, defaultValue = "简体中文") String language) {
        RecommendationResponse response = llmService.getRecommendation(request, language);
        return ApiResponse.success(response);
    }
}
