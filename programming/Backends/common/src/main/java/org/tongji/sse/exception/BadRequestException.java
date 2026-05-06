package org.tongji.sse.exception;

public class BadRequestException extends BusinessException {
    public BadRequestException(String message) {
        super(400, message);
    }
}
