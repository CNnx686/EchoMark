package org.tongji.sse.exception;

public class UnauthorizedException extends BusinessException{
    public UnauthorizedException() {
        super(401, "Unauthorized");
    }

    public UnauthorizedException(final String message) {
        super(401, message);
    }
}
