package org.tongji.sse.exception;

public class RateLimitExceededException extends BusinessException {
    public RateLimitExceededException() {
        super(429, "请求过于频繁，请稍后再试");
    }
}
