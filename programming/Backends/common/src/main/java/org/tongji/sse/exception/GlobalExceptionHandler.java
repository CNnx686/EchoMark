package org.tongji.sse.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.tongji.sse.dto.ApiResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 捕获所有业务异常
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    // 捕获文件上传过大的异常
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("上传文件过大，超过允许的大小限制");
    }

    // 捕获请求频率上限的异常
    @ExceptionHandler(RequestNotPermitted.class)
    public ApiResponse<?> handleRateLimit(RequestNotPermitted ex) {
        return ApiResponse.error(
                429,
                "请求过于频繁，请稍后再试"
        );
    }

    // 捕获请求失败率过高熔断的异常
    @ExceptionHandler(CallNotPermittedException.class)
    public ApiResponse<?> handleCircuitBreaker(CallNotPermittedException ex) {
        return ApiResponse.error(
                503,
                "服务暂时不可用（失败次数过多）"
        );
    }

    // 捕获资源未找到异常
    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<Void> handleNoResourceFoundException(NoResourceFoundException ex) {
        return ApiResponse.error(404, "资源未找到: " + ex.getResourcePath());
    }

    // 捕获未预料的异常（系统异常）
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOtherExceptions(Exception ex) {
        // 系统异常记录
        log.error("Unhandled exception occurred: ", ex);
        return ApiResponse.error(500, "服务器内部错误: "+ex.getMessage());
    }
}