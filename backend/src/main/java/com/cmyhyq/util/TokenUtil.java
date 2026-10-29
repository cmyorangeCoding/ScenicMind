package com.cmyhyq.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
public class TokenUtil {

    /**
     * 生成 token
     * @param userId
     * @param expireAtMs
     * @param secret
     * @return
     */
    public static String generateToken(long userId, long expireAtMs, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .expiration(new Date(expireAtMs))
                .issuedAt(new Date())
                .signWith(key)
                .compact();
    }

    /**
     * 解析 token
     * @param token
     * @param secret
     * @return
     */
    public static Claims verifyAndParse(String token, String secret) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("JWT 验证失败: {}", e.getMessage());
            return null;
        }
    }
}

