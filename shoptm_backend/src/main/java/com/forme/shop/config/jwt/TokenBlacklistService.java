package com.forme.shop.config.jwt;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 로그아웃(또는 강제 폐기)된 토큰의 jti를 들고 있는 메모리 블랙리스트
// - JWT는 발급하면 서버가 되돌릴 수 없는 방식이라, 로그아웃해도 토큰 자체는 만료 전까지 계속 유효함
// - 그래서 로그아웃 시 그 토큰의 jti를 여기 등록해두고, JwtFilter가 매 요청마다 확인함
// - 단일 서버 인스턴스 기준 구현. 서버를 여러 대로 늘리면 Redis 같은 공유 저장소로 바꿔야 함
// - 이 안에 있는 상태는 메모리뿐이라 서버 재시작하면 사라짐. jti 블랙리스트는 어차피 최대
//   토큰 만료 시간(jwt.expiration)만큼만 의미 있어서 재시작으로 사라져도 큰 문제가 아니지만,
//   회원 단위 전체무효화(정지/탈퇴)는 DB의 Member.deactivatedAt에도 함께 저장해두고
//   TokenBlacklistInitializer가 기동 시 이 맵에 다시 채워 넣어서, 재배포해도 밴 기록이
//   풀리지 않게 함(원래 메모리에만 두면 배포할 때마다 강퇴가 무효화되는 문제가 있었음)
@Component
public class TokenBlacklistService {

    // key: jti, value: 그 토큰의 만료 시각 (만료 지나면 굳이 안 들고 있어도 되므로 정리용)
    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    // key: 회원 email, value: "이 시각 이전에 발급된 토큰은 전부 무효" 기준 시각.
    // 로그아웃은 지금 쓰던 토큰 하나(jti)만 알면 되지만, 정지/탈퇴는 그 회원이 어느 기기에서
    // 몇 개의 토큰을 들고 있는지 서버가 알 방법이 없어서(jti를 세션별로 추적하지 않음) 개별
    // 무효화가 불가능함 — 대신 "이 순간 이후 발급된 토큰만 유효"라는 기준선을 세워서 한 번에 막음
    private final Map<String, Instant> revokedAllBeforeByEmail = new ConcurrentHashMap<>();

    // revokedAllBeforeByEmail 항목을 이보다 오래 들고 있을 필요는 없음 — 실제 토큰 수명(jwt.expiration,
    // 현재 24시간)보다 넉넉히 크게 잡아서, 나중에 설정값이 바뀌더라도(과거에는 더 긴 만료로 발급된
    // 토큰이 있었을 수 있음) 아직 안 끝났을 수 있는 토큰의 기준을 성급하게 지우지 않도록 함
    private static final long RETENTION_MS = Duration.ofDays(30).toMillis();

    public void revoke(String jti, Instant expiresAt) {
        cleanupExpired();
        revoked.put(jti, expiresAt);
    }

    public boolean isRevoked(String jti) {
        return revoked.containsKey(jti);
    }

    // 이 회원의 지금까지 발급된 토큰을 전부 무효화 (정지/탈퇴 시 호출) — 기준 시각은 지금
    public void revokeAllForEmail(String email) {
        revokeAllForEmail(email, Instant.now());
    }

    // 기준 시각을 직접 지정하는 버전 — 서버 기동 시 DB의 Member.deactivatedAt으로
    // 이 메모리 맵을 다시 채워 넣을 때(TokenBlacklistInitializer) 사용
    public void revokeAllForEmail(String email, Instant since) {
        cleanupExpiredEmailCutoffs();
        revokedAllBeforeByEmail.put(email, since);
    }

    // 서버 기동 시 DB에서 읽은 정지/탈퇴 기록을 한 번에 복원할 때 사용.
    // 여러 건을 반복 호출하며 매번 청소하면(정지/탈퇴 계정 수만큼 맵 전체를 반복 스캔) 계정이
    // 많아질수록 기동이 느려지므로, 전부 넣은 뒤 청소는 딱 한 번만 한다.
    public void restoreRevocations(Map<String, Instant> cutoffsByEmail) {
        revokedAllBeforeByEmail.putAll(cutoffsByEmail);
        cleanupExpiredEmailCutoffs();
    }

    // 이 토큰이 그 회원의 "전체 무효화 기준 시각" 이전에 발급됐는지
    public boolean isRevokedForEmail(String email, Instant issuedAt) {
        Instant cutoff = revokedAllBeforeByEmail.get(email);
        return cutoff != null && !issuedAt.isAfter(cutoff);
    }

    // 이미 만료된 토큰까지 메모리에 계속 들고 있을 필요는 없어서 청소
    private void cleanupExpired() {
        Instant now = Instant.now();
        revoked.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }

    // 기준 시각이 RETENTION_MS보다 더 오래된 항목은 그 무렵 발급됐을 토큰이 이미 전부
    // 자연 만료됐을 것이므로 정리 (정지/탈퇴 계정이 쌓일수록 맵이 무한정 커지는 것 방지)
    private void cleanupExpiredEmailCutoffs() {
        Instant oldestRelevant = Instant.now().minusMillis(RETENTION_MS);
        revokedAllBeforeByEmail.entrySet().removeIf(entry -> entry.getValue().isBefore(oldestRelevant));
    }
}
