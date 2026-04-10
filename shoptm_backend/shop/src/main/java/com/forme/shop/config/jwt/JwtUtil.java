package com.forme.shop.config.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component  // 스프링 빈으로 등록
public class JwtUtil {

    private final Key key;
    private final long expiration;

    // application.yml의 jwt.secret, jwt.expiration 값을 주입받음
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());  // 비밀키 생성
        this.expiration = expiration;
    }

    // JWT 토큰 생성
    // email과 role을 토큰에 담아서 반환
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)                                      // 토큰 주인 (email)
                .claim("role", role)                                    // 추가 정보 (권한)
                .setIssuedAt(new Date())                                // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + expiration))  // 만료 시간
                .signWith(key)                                         // 서명
                .compact();
    }

    // 토큰에서 이메일 추출
    public String getEmail(String token) {
        return getClaims(token).getSubject();
    }

    // 토큰에서 권한 추출
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // 토큰 유효성 검증
    // 만료되거나 변조된 토큰이면 false 반환
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 토큰에서 Claims(데이터) 추출
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}