package com.forme.shop.config.jwt;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 로그아웃(또는 강제 폐기)된 토큰의 jti를 들고 있는 메모리 블랙리스트
// - JWT는 발급하면 서버가 되돌릴 수 없는 방식이라, 로그아웃해도 토큰 자체는 만료 전까지 계속 유효함
// - 그래서 로그아웃 시 그 토큰의 jti를 여기 등록해두고, JwtFilter가 매 요청마다 확인함
// - 단일 서버 인스턴스 기준 구현. 서버를 여러 대로 늘리면 Redis 같은 공유 저장소로 바꿔야 함
@Component
public class TokenBlacklistService {

    // key: jti, value: 그 토큰의 만료 시각 (만료 지나면 굳이 안 들고 있어도 되므로 정리용)
    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    public void revoke(String jti, Instant expiresAt) {
        cleanupExpired();
        revoked.put(jti, expiresAt);
    }

    public boolean isRevoked(String jti) {
        return revoked.containsKey(jti);
    }

    // 이미 만료된 토큰까지 메모리에 계속 들고 있을 필요는 없어서 청소
    private void cleanupExpired() {
        Instant now = Instant.now();
        revoked.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
