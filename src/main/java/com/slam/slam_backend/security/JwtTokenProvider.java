package com.slam.slam_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    // ✅ 토큰 유효 기간 설정 (24시간)
    private final long EXPIRATION_TIME = 1000L * 60 * 60 * 24;

    private Key key;

    @PostConstruct
    protected void init() {
        try {
            if (secret == null || secret.length() < 32) {
                // Fallback: generate a runtime key to allow the app to start
                // NOTE: Tokens will be invalidated on restart. Set JWT_SECRET_KEY (>=32 chars) in production.
                key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            } else {
                key = Keys.hmacShaKeyFor(secret.getBytes());
            }
        } catch (Exception e) {
            // As a last resort, generate a key to prevent startup failure
            key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
    }

    // ✅ 토큰을 생성하는 메소드 추가
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}