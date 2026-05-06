package org.tongji.sse.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tongji.sse.dto.LlmRequest;
import org.tongji.sse.dto.LlmResponse;
import org.tongji.sse.dto.RecommendationResponse;
import org.tongji.sse.dto.external.DeepSeekRequest;
import org.tongji.sse.dto.external.DeepSeekResponse;
import org.tongji.sse.entity.LlmAudio;
import org.tongji.sse.entity.UserPersona;
import org.tongji.sse.repository.LlmAudioRepository;
import org.tongji.sse.repository.UserPersonaRepository;
import org.tongji.sse.security.SecurityUtil;
import org.tongji.sse.service.LlmService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class LlmServiceImpl implements LlmService {

    @Value("${llm.api.url}")
    private String apiUrl;

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.model}")
    private String model;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final UserPersonaRepository userPersonaRepository;
    private final LlmAudioRepository audioRepository;
    private final StringRedisTemplate redisTemplate;

    public LlmServiceImpl(ObjectMapper objectMapper,
                          UserPersonaRepository userPersonaRepository,
                          LlmAudioRepository audioRepository,
                          StringRedisTemplate redisTemplate) {
        this.objectMapper = objectMapper;
        this.userPersonaRepository = userPersonaRepository;
        this.audioRepository = audioRepository;
        this.redisTemplate = redisTemplate;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 生成LLM响应
     * @param request LlmRequest对象，包含用户的提示信息
     * @return LlmResponse对象，包含LLM生成的响应内容
     */    
    @Override
    public LlmResponse generateResponse(LlmRequest request) {
        try {
            DeepSeekRequest.Thinking thinking = null;
            if (Boolean.FALSE.equals(request.getEnableThinking())) {
                thinking = DeepSeekRequest.Thinking.builder().type("disabled").build();
            }

            DeepSeekRequest deepSeekRequest = DeepSeekRequest.builder()
                    .model(model)
                    .messages(Collections.singletonList(
                            DeepSeekRequest.Message.builder()
                                    .role("user")
                                    .content(request.getPrompt())
                                    .build()
                    ))
                    .thinking(thinking)
                    .stream(false)
                    .temperature(1.0)
                    .topP(1.0)
                    .maxTokens(4096)
                    .responseFormat(DeepSeekRequest.ResponseFormat.builder().type("text").build())
                    .build();

            String jsonBody = objectMapper.writeValueAsString(deepSeekRequest);
            
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
            
            Request httpRequest = new Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Unexpected code " + response);
                }
                
                if (response.body() == null) {
                    throw new RuntimeException("Response body is null");
                }

                String responseBody = response.body().string();
                DeepSeekResponse deepSeekResponse = objectMapper.readValue(responseBody, DeepSeekResponse.class);

                if (deepSeekResponse.getChoices() == null || deepSeekResponse.getChoices().isEmpty()) {
                     throw new RuntimeException("No choices in response");
                }

                DeepSeekResponse.Choice choice = deepSeekResponse.getChoices().get(0);
                
                return LlmResponse.builder()
                        .requestId(deepSeekResponse.getId())
                        .statusCode(response.code())
                        .chainOfThought(choice.getMessage().getReasoningContent())
                        .content(choice.getMessage().getContent())
                        .build();
            }

        } catch (IOException e) {
            throw new RuntimeException("Error calling LLM API", e);
        }
    }

    /**
     * 生成LLM流式响应
     * @param request LlmRequest对象
     * @return SseEmitter用于流式传输
     */
    @Override
    public void generateResponseStream(LlmRequest request, SseEmitter emitter) {
        CompletableFuture.runAsync(() -> {
            try {
                DeepSeekRequest.Thinking thinking = null;
                if (Boolean.FALSE.equals(request.getEnableThinking())) {
                    thinking = DeepSeekRequest.Thinking.builder().type("disabled").build();
                }

                DeepSeekRequest deepSeekRequest = DeepSeekRequest.builder()
                        .model(model)
                        .messages(Collections.singletonList(
                                DeepSeekRequest.Message.builder()
                                        .role("user")
                                        .content(request.getPrompt())
                                        .build()
                        ))
                        .thinking(thinking)
                        .stream(true)
                        .streamOptions(DeepSeekRequest.StreamOptions.builder().includeUsage(false).build())
                        .temperature(1.0)
                        .topP(1.0)
                        .maxTokens(4096)
                        .responseFormat(DeepSeekRequest.ResponseFormat.builder().type("text").build())
                        .build();

                String jsonBody = objectMapper.writeValueAsString(deepSeekRequest);
                RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

                Request httpRequest = new Request.Builder()
                        .url(apiUrl)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "application/json")
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .build();

                try (Response response = client.newCall(httpRequest).execute()) {
                    if (!response.isSuccessful()) {
                        emitter.send(SseEmitter.event().name("error").data("Error: " + response.code()));
                        emitter.complete();
                        return;
                    }

                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody == null) {
                            emitter.complete();
                            return;
                        }

                        BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                if ("[DONE]".equals(data)) {
                                    emitter.send(SseEmitter.event().data("[DONE]"));
                                    emitter.complete();
                                    break;
                                }

                                try {
                                    DeepSeekResponse chunk = objectMapper.readValue(data, DeepSeekResponse.class);
                                    if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                                        DeepSeekResponse.Choice choice = chunk.getChoices().get(0);
                                        DeepSeekResponse.Message delta = choice.getDelta();
                                        
                                        if (delta != null) {
                                            Map<String, Object> eventData = new HashMap<>();
                                            if (delta.getContent() != null) {
                                                eventData.put("content", delta.getContent());
                                            }
                                            if (delta.getReasoningContent() != null) {
                                                eventData.put("chainOfThought", delta.getReasoningContent());
                                            }
                                            
                                            // 仅在有数据时发送事件
                                            if (!eventData.isEmpty()) {
                                                emitter.send(SseEmitter.event().data(eventData));
                                            }
                                        }
                                    } else if (chunk.getUsage() != null) {
                                         Map<String, Object> usageData = new HashMap<>();
                                         usageData.put("usage", chunk.getUsage());
                                         emitter.send(SseEmitter.event().data(usageData));
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error parsing streaming data: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
    }

    /**
     * 获取智能推荐
     * @param request HTTP请求
     * @return 推荐结果
     */
    @SuppressWarnings("null")
    @Override
    public RecommendationResponse getRecommendation(HttpServletRequest request, String language) {
        try {
            Long userId = SecurityUtil.getUserIdOrThrow(request);
            String cacheKey = "RECOMMENDATION:" + userId + ":" + language;

            // 0. 检查缓存
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                return objectMapper.readValue(cachedValue, RecommendationResponse.class);
            }

            // 1. 获取用户画像
            UserPersona userPersona = userPersonaRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User persona not found for id: " + userId));

            // 2. 解析画像，提取标签和关键词
            JsonNode personaNode = objectMapper.readTree(userPersona.getPersonaJson());
            Set<String> tags = new HashSet<>();
            
            // 从 tagWeights 提取标签
            JsonNode tagWeightsNode = personaNode.path("tagWeights");
            if (tagWeightsNode.isObject()) {
                Iterator<String> fieldNames = tagWeightsNode.fieldNames();
                while (fieldNames.hasNext()) {
                    tags.add(fieldNames.next());
                }
            }
            
            // 从 keywordWeights 提取关键词
            JsonNode keywordWeightsNode = personaNode.path("keywordWeights");
            if (keywordWeightsNode.isObject()) {
                Iterator<String> fieldNames = keywordWeightsNode.fieldNames();
                while (fieldNames.hasNext()) {
                    tags.add(fieldNames.next());
                }
            }

            // 3. 查询音频数据库，获取候选音频
            if (tags.isEmpty()) {
                return RecommendationResponse.builder()
                        .reason("当前个人信息不足，无法为您生成个性化推荐，请先进行一定浏览后再获取。")
                        .audioIds(Collections.emptyList())
                        .build();
            }

            List<LlmAudio> candidates = audioRepository.findDistinctByTagsInAndStatusAndUserIdNot(tags, "PUBLISHED", userId);
            
            // 限制候选音频数量，避免过多
            if (candidates.size() > 20) {
                candidates = candidates.subList(0, 20);
            }

            // 4. 构建提示词
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("你是我的私人音频推荐助手。请你基于我的个人资料和兴趣，为我挑选一些音频。\n\n");
            
            promptBuilder.append("个人资料:\n");
            promptBuilder.append(userPersona.getPersonaJson()).append("\n\n");
            
            promptBuilder.append("候选音频列表:\n");
            for (LlmAudio audio : candidates) {
                promptBuilder.append(String.format("- ID: %d, 标题: %s, 简介: %s, 标签: %s\n", 
                        audio.getId(), audio.getTitle(), audio.getDescription(), audio.getTags()));
            }
            
            promptBuilder.append("\n请根据个人资料，从候选列表中推荐最适合的音频并给出理由。\n");
            promptBuilder.append("要求：\n");
            promptBuilder.append("1. reason是推荐理由字段，audioIds是推荐音频ID列表字段。\n");
            promptBuilder.append("2. 返回格式必须是严格的JSON格式，包含且仅包含两个字段：'reason' (字符串) 和 'audioIds' (数字列表)。\n");
            promptBuilder.append("3. 不要包含Markdown代码块标记（如 ```json），直接返回JSON字符串。\n");
            promptBuilder.append(String.format("4. 使用%s输出。\n", language));

            // 5. 调用LLM服务
            LlmRequest llmRequest = new LlmRequest();
            llmRequest.setPrompt(promptBuilder.toString());
            
            LlmResponse llmResponse = generateResponse(llmRequest);
            
            // 6. 解析LLM响应
            String content = llmResponse.getContent();
            // 去除可能的代码块标记
            content = content.replace("```json", "").replace("```", "").trim();
            
            RecommendationResponse response = objectMapper.readValue(content, RecommendationResponse.class);
            
            // 7. 缓存结果
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), 1, TimeUnit.DAYS);
            
            return response;

        } catch (Exception e) {
            throw new RuntimeException("LLM服务出错，请稍后重试: " + e.getMessage(), e);
        }
    }
}
