package com.seckill.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    // 自动生成符合HS256标准的安全密钥
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    // 24小时过期
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000L;

    // 生成Token
    public String generateToken(Map<String, Object> claims) {
        try {
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                    .signWith(SECRET_KEY)
                    .compact();
            log.debug("Token 生成成功, username: {}", claims.get("username"));
            return token;
        } catch (Exception e) {
            log.error("Token 生成失败: {}", e.getMessage(), e);
            throw new RuntimeException("Token 生成失败", e);
        }
    }

    // 生成Token（简化版，只需传入userId）
    public static String generateToken(Long userId) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", userId);
        
        try {
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                    .signWith(SECRET_KEY)
                    .compact();
            log.debug("Token 生成成功, userId: {}", userId);
            return token;
        } catch (Exception e) {
            log.error("Token 生成失败: {}", e.getMessage(), e);
            throw new RuntimeException("Token 生成失败", e);
        }
    }

    // 解析Token
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.debug("Token 已过期: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.warn("Token 解析失败: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Token 解析异常: {}", e.getMessage(), e);
            return null;
        }
    }

    // ===================== 拦截器需要的新增方法 =====================
    // 获取用户ID
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("userId", Long.class) : null;
    }

    // 判断Token是否过期
    public boolean isExpired(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return true;
        return claims.getExpiration().before(new Date());
    }

    // 验证Token有效性
    public boolean validateToken(String token) {
        boolean isValid = parseToken(token) != null;
        if (!isValid) {
            log.debug("Token 验证失败");
        }
        return isValid;
    }
}