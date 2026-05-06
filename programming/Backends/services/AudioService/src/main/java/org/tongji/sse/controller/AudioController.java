package org.tongji.sse.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tongji.sse.dto.*;
import org.tongji.sse.redisUtil.apiCache.ApiCache;
import org.tongji.sse.redisUtil.idempotent.Idempotent;
import org.tongji.sse.redisUtil.rateLimit.RedisRateLimit;
import org.tongji.sse.service.AudioService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RateLimiter(name = "audioApiRateLimiter")
@CircuitBreaker(name = "audioApiCircuitBreaker")
@RedisRateLimit(name = "audio")
@RequestMapping("/api/audio")
public class AudioController {

    private final AudioService service;

    @Idempotent(name = "audio-upload")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AudioResponseDto> upload(HttpServletRequest request,
                                                @RequestPart("file") MultipartFile file,
                                                @ModelAttribute UploadAudioRequest req) {
        return ApiResponse.success(service.upload(request, file, req));
    }

    @PostMapping(value = "/photo/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> photo(HttpServletRequest request,
                                     @RequestPart("file") MultipartFile file){
        return ApiResponse.success(service.uploadPhoto(request, file));
    }

    @Idempotent(name = "audio-general")
    @PostMapping("/{id}/publish")
    public ApiResponse<AudioResponseDto> publish(HttpServletRequest request,
                                                 @PathVariable Long id,
                                                 @RequestBody PublishAudioRequest req) {
        return ApiResponse.success(service.publish(request, id, req));
    }

    @PostMapping("/{id}/update")
    public ApiResponse<AudioResponseDto> updateAudio(HttpServletRequest request,
                                                     @PathVariable Long id,
                                                     @RequestBody UpdateAudioRequest req){
        return ApiResponse.success(service.updateAudio(request, id, req));
    }

    @Idempotent(name = "audio-general")
    @PostMapping("/{id}/hide")
    public ApiResponse<AudioResponseDto> hide(HttpServletRequest request,
                                              @PathVariable Long id,
                                              @RequestParam boolean hidden) {
        return ApiResponse.success(service.hide(request, id, hidden));
    }

    @Idempotent(name = "audio-general")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        service.delete(request, id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    @ApiCache(name = "general")
    public ApiResponse<AudioResponseDto> get(HttpServletRequest request, @PathVariable Long id) {
        return ApiResponse.success(service.get(request, id));
    }

    @PostMapping("/list")
    @ApiCache(name = "general")
    public ApiResponse<List<AudioResponseDto>> getMulti(HttpServletRequest request, @RequestBody List<Long> ids) {
        return ApiResponse.success(service.getMulti(request, ids));
    }

    @GetMapping("/nearby")
    @ApiCache(name = "general")
    public ApiResponse<List<AudioResponseDto>> getNearbyAudio(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double distance
    ){
        return ApiResponse.success(service.getNearbyAudio(latitude, longitude, distance));
    }

    @GetMapping("/search")
    public ApiResponse<List<AudioResponseDto>> search(HttpServletRequest request, @RequestParam String keyword) {
        return ApiResponse.success(service.search(request, keyword));
    }

    @GetMapping("/recommendation")
    public ApiResponse<List<AudioResponseDto>> getRecommendation(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(service.getRecommendation(limit));
    }

    @GetMapping("/all/{userId}")
    public ApiResponse<List<AudioResponseDto>> getAllUserAudios(HttpServletRequest request, @PathVariable Long userId){
        return ApiResponse.success(service.getAllUserAudios(request, userId));
    }
}
