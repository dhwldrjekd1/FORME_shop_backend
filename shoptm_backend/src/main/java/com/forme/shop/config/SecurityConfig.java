package com.forme.shop.config;

import com.forme.shop.config.jwt.JwtAuthEntryPoint;
import com.forme.shop.config.jwt.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration        // 스프링 설정 클래스임을 선언
@EnableWebSecurity    // Spring Security 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;                  // JWT 토큰 검증 필터
    private final JwtAuthEntryPoint jwtAuthEntryPoint;  // 인증 실패 처리

    // BCryptPasswordEncoder 를 스프링 빈으로 등록
    // MemberService 에서 PasswordEncoder 주입받을 때 이 빈이 사용됨
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API 라서 CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // JWT 사용이라 세션 사용 안함 (STATELESS)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 인증되지 않은 사용자가 보호된 API 접근 시 401 반환
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthEntryPoint)
                )

                // Spring Security 기본 헤더 중 Cache-Control(no-cache, no-store...)은
                // API 응답뿐 아니라 정적 리소스(이미지/JS/CSS)에도 무조건 붙어서 캐싱을 막아버림.
                // WebConfig 에서 리소스별로 캐싱 정책을 직접 설정하므로 여기서는 끔.
                .headers(headers -> headers
                        .cacheControl(cache -> cache.disable())
                )

                // URL 별 접근 권한 설정 (순서 중요 - 구체적인 것부터 작성)
                .authorizeHttpRequests(auth -> auth
                        // 회원가입, 로그인은 누구나 접근 가능
                        .requestMatchers("/api/register", "/api/login").permitAll()
                        // 상품 조회는 비로그인도 가능 (등록/수정/삭제는 전부 /api/admin/products/**
                        // 아래에 있어 바로 아래 "/api/admin/**" 규칙으로 걸림 — 그럼에도 다른
                        // 조회 전용 경로들(게시판/댓글/Q&A/설정)과 똑같이 GET으로 범위를 좁혀서,
                        // 나중에 비관리자 쓰기 엔드포인트가 /api/products/** 아래 실수로 추가되는
                        // 경우까지 이 permitAll이 자동으로 열어주지 않게 한다(교차검증에서 발견).
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        // 카테고리 조회도 같은 이유로 GET만 비로그인 허용
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        // 게시판 조회는 비로그인도 가능 (수정/삭제는 로그인 필요 - 아래 authenticated()로 폴백)
                        .requestMatchers(HttpMethod.GET, "/api/boards/**").permitAll()
                        // 댓글 조회는 비로그인도 가능 (수정/삭제는 로그인 필요)
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        // Q&A 조회는 비로그인도 가능 (수정/삭제는 로그인 필요)
                        .requestMatchers(HttpMethod.GET, "/api/qna/**").permitAll()
                        // FAQ 조회는 비로그인도 가능
                        .requestMatchers("/api/faq/**").permitAll()
                        // 사이트 설정 조회는 비로그인도 가능
                        .requestMatchers(HttpMethod.GET, "/api/settings/**").permitAll()
                        // 관리자만 접근 가능
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        // 방문자 페이지뷰 기록(POST /analytics/track)은 비로그인 방문자를 포함해
                        // 모든 사용자의 페이지 이동마다 호출되는 트래킹용이라 계속 열어둬야 함
                        // (utils/pageTracker.js — 로그인 여부와 무관하게 모든 방문에서 호출됨).
                        .requestMatchers(HttpMethod.POST, "/api/analytics/track").permitAll()
                        // 방문자 분석 집계 조회(요약/일별/회원별/상품별 등)는 관리자 전용 — 경로가
                        // /api/admin/** 밑이 아니라서 이 줄이 없으면 바로 아래 authenticated()에
                        // 걸려 로그인만 한 일반 회원도 접근 가능했음(교차검증에서 발견).
                        .requestMatchers(HttpMethod.GET, "/api/analytics/**").hasAuthority("ROLE_ADMIN")
                        // 그 외 /api/** 는 로그인 필요 (장바구니, 주문, 마이페이지 등)
                        .requestMatchers("/api/**").authenticated()
                        // 나머지(SPA 정적 리소스, Vue Router 경로) 는 누구나 접근 가능
                        // → "/", "/index.html", "/assets/**", "/products/123" 같은 SPA 라우트 모두 통과
                        .anyRequest().permitAll()
                )

                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                // 모든 요청에서 JWT 토큰을 먼저 검증
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                // ApiNoStoreFilter도 같은 지점(ExceptionTranslationFilter보다 앞)에 등록 — 인증
                // 실패(401)로 체인이 여기서 바로 끝나도 Cache-Control 헤더는 이미 설정돼 있어야 함
                //
                // 주의: addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)로 등록된
                // 필터끼리는 Spring Security가 서로 같은 순서값을 매기므로, 이 둘의 실행 순서는
                // 오직 "코드에 등록한 순서"로만 정해진다(교차검증에서 발견) — ApiNoStoreFilter가
                // AnalyticsRateLimitFilter보다 반드시 먼저 실행돼야(레이트리밋에 걸려 429로 바로
                // 끝나는 응답에도 Cache-Control: no-store가 붙어있어야 하므로) 이 두 줄의 순서를
                // 절대 바꾸지 말 것.
                .addFilterBefore(new ApiNoStoreFilter(), UsernamePasswordAuthenticationFilter.class)
                // /api/analytics/track은 permitAll(위 참고)이라 인증 필터를 거치지 않으므로,
                // 레이트리밋도 같은 지점에 등록해야 인증 여부와 무관하게 항상 적용된다.
                .addFilterBefore(new AnalyticsRateLimitFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}