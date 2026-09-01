package com.housedesign.security;

import com.housedesign.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 工具：负责令牌的签发（generateToken）与解析（parseUserId）。
 * 签名密钥取自配置 app.jwt.secret。
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final AppProperties appProperties;

    /** 根据配置的 secret 构造 HMAC-SHA256 签名密钥。 */
    private Key key() {
        byte[] bytes = appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }

    /** 为用户签发 JWT：subject 为 userId，claim 携带 username，设置签发与过期时间。 */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + appProperties.getJwt().getExpirationMs());
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析 JWT 并返回其中的 userId（subject）。校验签名与时效性由 jjwt 内部完成。 */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.valueOf(claims.getSubject());
    }
}
