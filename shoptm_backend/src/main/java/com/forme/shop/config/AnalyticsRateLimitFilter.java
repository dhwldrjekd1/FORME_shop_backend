package com.forme.shop.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

// /api/analytics/track은 비로그인 방문자도 호출해야 해서 SecurityConfig에서 permitAll인데,
// 인증도 없고 호출 횟수에 아무 제한도 없어서 누구나 이 엔드포인트만 반복 호출하는 것으로
// page_views 테이블을 무한정 불려 DB 저장공간을 소모시키거나, 관리자 통계(조회수/체류시간
// 평균 등)를 의미 없는 값으로 오염시킬 수 있었다(다른 방향에서 버그를 찾다가 발견). 이
// 앱이 단일 인스턴스로만 운영되는 규모(배포 과정 참고)라 Redis 등 외부 저장소를 쓰는
// 분산 레이트리밋은 과한 설계라, IP별 메모리 내 고정 윈도우 카운터로 충분히 막는다.
//
// Caddyfile(forme.dyy.kr)이 이 앱(127.0.0.1:8302, 외부에서 직접 접근 불가 — application.yml의
// server.address 참고) 앞의 유일한 리버스 프록시라서, X-Forwarded-For는 Caddy가 항상 실제
// 클라이언트 IP로 채워 보내는 값이라 그대로 신뢰해도 된다(이 경로를 거치지 않고는 이 필터에
// 도달할 방법이 없음).
//
// ApiNoStoreFilter와 동일한 이유로 @Component를 쓰지 않는다 — SecurityConfig에서 new로 직접
// 만들어 보안 체인에만 등록해야, Spring Boot의 전역 필터 자동 등록과 겹쳐 두 번 실행되는 걸 막는다.
public class AnalyticsRateLimitFilter extends OncePerRequestFilter {

    private static final int WINDOW_MILLIS = 60_000;      // 1분 고정 윈도우
    // 회사/학교망처럼 여러 방문자가 같은 공인 IP(NAT)를 공유하면 이 한도를 실제 어뷰징 없이도
    // 넘길 수 있다(교차검증에서 발견) — 넉넉히 60으로 잡아 그런 경우의 오탐 가능성을 줄인다.
    // 그래도 걸리는 극소수 케이스는 그 요청의 페이지뷰 하나가 누락되는 정도(AnalyticsService
    // 통계에 미미한 영향)라, 그 이상으로 정교하게 구분하는 건(세션/유저 단위 등 — 비로그인
    // 방문자 상대로는 애초에 불가능) 이 수준의 피해에 비해 과한 설계.
    private static final int MAX_REQUESTS_PER_WINDOW = 60;
    private static final long SWEEP_INTERVAL_MILLIS = 5 * 60_000L; // 5분마다 오래된 IP 항목 정리(이 맵 자체가 무한정 커지는 것도 막아야 함)

    // 모든 읽기/쓰기가 아래 doFilterInternal의 synchronized(window) 블록 안에서만 일어나므로
    // AtomicLong 같은 CAS 기반 필드는 이 lock 위에 아무 이득도 더하지 못하는 중복 장치다 —
    // 평범한 long 필드로 충분하다(교차검증에서 발견).
    private static class Window {
        long windowStart = System.currentTimeMillis();
        long count = 0;
    }

    private final ConcurrentHashMap<String, Window> windowsByIp = new ConcurrentHashMap<>();
    private volatile long lastSweep = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!("POST".equalsIgnoreCase(request.getMethod()) && "/api/analytics/track".equals(request.getRequestURI()))) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        long now = System.currentTimeMillis();
        sweepIfDue(now);

        Window window = windowsByIp.computeIfAbsent(ip, k -> new Window());
        synchronized (window) {
            if (now - window.windowStart >= WINDOW_MILLIS) {
                window.windowStart = now;
                window.count = 0;
            }
            window.count++;
            if (window.count > MAX_REQUESTS_PER_WINDOW) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    // 동시에 여러 요청이 스윕 시점에 걸려 중복 실행되더라도(이 메서드 자체는 synchronized가
    // 아님) removeIf가 중복 호출되는 정도일 뿐 데이터 정합성엔 문제가 없어, 그 드문 중복 실행을
    // 막으려고 따로 락을 걸 필요는 없다.
    private synchronized void sweepIfDue(long now) {
        if (now - lastSweep < SWEEP_INTERVAL_MILLIS) return;
        lastSweep = now;
        windowsByIp.entrySet().removeIf(entry -> now - entry.getValue().windowStart >= WINDOW_MILLIS);
    }

    private String clientIp(HttpServletRequest request) {
        // Caddy의 reverse_proxy는 요청에 이미 X-Forwarded-For가 있으면 그 값을 덮어쓰지 않고
        // 자신이 관찰한 IP를 뒤에 덧붙인다 — 즉 클라이언트가 이 헤더에 아무 값이나 미리 넣어
        // 보내면("X-Forwarded-For: 1.2.3.4") 맨 앞 값은 클라이언트가 직접 조작할 수 있어
        // 신뢰할 수 없다(교차검증에서 발견 — 맨 앞 값을 쓰면 매 요청마다 가짜 IP를 바꿔 보내는
        // 것만으로 이 레이트리밋을 그냥 우회할 수 있었음). 이 앱 앞에는 Caddy 하나만 있으므로
        // (클래스 주석 참고), 그 Caddy가 직접 덧붙인 마지막 값만 믿는다.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
