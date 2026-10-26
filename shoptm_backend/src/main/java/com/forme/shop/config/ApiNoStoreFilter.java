package com.forme.shop.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// SecurityConfig가 Spring Security의 기본 Cache-Control 헤더(no-cache/no-store...)를 전역으로
// 꺼둬서(정적 리소스에 설정한 WebConfig의 캐싱 정책이 덮어써지는 걸 막기 위함 — SecurityConfig
// 주석 참고) /api/** 응답엔 Cache-Control이 전혀 안 붙는 상태로 남아있었음. 회원정보·주문내역·
// 관리자 데이터처럼 민감한 응답이 브라우저 캐시/뒤로가기 캐시나 중간 프록시에 남을 수 있는
// 틈이라 /api/** 전용으로 no-store를 다시 명시적으로 붙인다.
//
// HandlerInterceptor가 아니라 서블릿 Filter로 구현한 이유: 인증 실패(401)/권한 없음(403)은
// Spring Security 필터체인 안에서 DispatcherServlet까지 가지도 못하고 바로 응답을 끝내버려서,
// HandlerInterceptor(DispatcherServlet이 호출하는 더 늦은 단계)는 그 경우 전혀 실행되지 않는다
// (교차검증에서 발견). SecurityFilterChainConfigurer보다 먼저 실행되도록 등록해, 그 뒤 체인이
// 어디서 응답을 끝내든 헤더는 이미 설정돼 있도록 한다(SecurityConfig의 addFilterBefore 참고).
//
// @Component로 등록하지 않는다 — 의존성이 없는 단순한 필터라 Spring Boot가 이 빈을 전역
// 서블릿 필터로 자동 등록하는 것까지 겹치면 SecurityConfig의 addFilterBefore로 보안 체인
// 안에도 등록되는 것과 두 번 실행될 수 있다 — 그럴 이유가 없으므로 SecurityConfig에서 new로
// 직접 만들어 보안 체인에만 등록한다.
public class ApiNoStoreFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store");
        }
        filterChain.doFilter(request, response);
    }
}
