package com.dataplatform.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 
 * @author dataplatform
 */
@Component
public class JwtUtil {
    private String secret = "data-platform-secret-key-2024-must-be-at-least-256-bits";
    private long expiration = 1800000L;
    private long refreshThreshold = 600000L;
    private SecretKey secretKey;

    @Value("${jwt.secret:data-platform-secret-key-2024-must-be-at-least-256-bits}")
    public void setSecret(String secret) { this.secret = secret; this.secretKey = null; }

    @Value("${jwt.expiration:1800000}")
    public void setExpiration(long expiration) { this.expiration = expiration; }

    @Value("${jwt.refreshThreshold:600000}")
    public void setRefreshThreshold(long refreshThreshold) { this.refreshThreshold = refreshThreshold; }

    private SecretKey getSigningKey() {
        if (secretKey == null) {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 32) {
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
                keyBytes = paddedKey;
            }
            secretKey = Keys.hmacShaKeyFor(keyBytes);
        }
        return secretKey;
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        return createToken(claims, username);
    }

    public String generateToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        if (userId != null) claims.put("userId", userId);
        return createToken(claims, username);
    }

    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            Object userId = claims.get("userId");
            if (userId instanceof Number) return ((Number) userId).longValue();
            return null;
        } catch (Exception e) { return null; }
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }

    public String getUsernameFromToken(String token) { return parseToken(token).getSubject(); }

    public boolean validateToken(String token) {
        try { return !parseToken(token).getExpiration().before(new Date()); }
        catch (Exception e) { return false; }
    }

    public boolean shouldRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            long remainingTime = claims.getExpiration().getTime() - System.currentTimeMillis();
            return remainingTime > 0 && remainingTime < refreshThreshold;
        } catch (Exception e) { return false; }
    }

    public String refreshToken(String token) {
        try {
            String username = getUsernameFromToken(token);
            Long userId = getUserIdFromToken(token);
            return generateToken(username, userId);
        } catch (Exception e) { return null; }
    }
}
