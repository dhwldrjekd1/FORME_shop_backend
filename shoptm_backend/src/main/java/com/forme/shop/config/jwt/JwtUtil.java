package com.forme.shop.config.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component  // 스프링 빈으로 등록
public class JwtUtil {

    // 토큰을 담아둘 httpOnly 쿠키 이름 — 자바스크립트(document.cookie)로는 절대 못 읽음
    public static final String COOKIE_NAME = "auth_token";

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
    // jti(토큰 고유 ID)를 발급해서, 로그아웃 시 이 토큰만 콕 집어 폐기할 수 있게 함 (TokenBlacklistService)
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())                    // jti: 토큰 고유 ID
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

    // 토큰 고유 ID(jti) 추출 — 블랙리스트 등록/조회 키로 사용
    public String getJti(String token) {
        return getClaims(token).getId();
    }

    // 토큰 만료 시각 추출 — 블랙리스트 항목을 언제까지 들고 있으면 되는지 판단용
    public Date getExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    // 로그인 성공 시 내려줄 쿠키 생성
    // httpOnly: 자바스크립트가 못 읽음 (XSS로 토큰 탈취 방지)
    // secure: HTTPS에서만 전송 (forme.dyy.kr은 Caddy가 TLS 종료 후 프록시하므로 항상 해당)
    // sameSite=Lax: 다른 사이트에서 이 쿠키를 실어 요청 위조(CSRF)하는 것 방지
    public ResponseCookie buildAuthCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api")
                .maxAge(Duration.ofMillis(expiration))
                .build();
    }

    // 로그아웃 시 브라우저에서 쿠키를 즉시 지우기 위한 만료 쿠키
    public ResponseCookie buildExpiredAuthCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api")
                .maxAge(0)
                .build();
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