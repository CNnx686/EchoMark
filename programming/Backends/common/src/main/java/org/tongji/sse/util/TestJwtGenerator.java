package org.tongji.sse.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

// 用这个工具可以生成一个无限有效时长的token，省的每次都去开AuthService然后登录，仅用于开发测试
public class TestJwtGenerator {

    private static final String SECRET_KEY =
            "ThisIsASecretKeyForJwtGenerationMustBeLongEnough123!";
    private static final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    public static String generateDevToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();  // 不设置过期时间
    }

    public static void main(String[] args) {
        System.out.println("Dev Token:");
        System.out.println(generateDevToken(6L));
    }
}
