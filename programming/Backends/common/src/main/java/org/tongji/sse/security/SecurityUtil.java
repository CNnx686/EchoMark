package org.tongji.sse.security;

import jakarta.servlet.http.HttpServletRequest;
import org.tongji.sse.exception.AccessDeniedException;

public class SecurityUtil {
    public static Long getUserIdOrThrow(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || !JwtUtil.validateToken(token))
            throw new AccessDeniedException("无效或缺失 token");
        return JwtUtil.getUserId(token);
    }

    public static Long getUserIdOrNull(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || !JwtUtil.validateToken(token))
            return null;
        return JwtUtil.getUserId(token);
    }

    private static String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer "))
            return authHeader.substring(7);
        return null;
    }
}
