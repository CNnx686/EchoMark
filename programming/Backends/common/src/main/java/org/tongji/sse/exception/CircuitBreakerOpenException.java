package org.tongji.sse.exception;

public class CircuitBreakerOpenException extends BusinessException {

    public CircuitBreakerOpenException() {
        super(503, "系统繁忙，服务暂时不可用");
    }
}
