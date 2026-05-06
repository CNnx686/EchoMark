package org.tongji.sse.exception;

public class IdempotentException extends BusinessException {

    public IdempotentException() {
        super(429, "请勿重复提交请求");
    }
}

