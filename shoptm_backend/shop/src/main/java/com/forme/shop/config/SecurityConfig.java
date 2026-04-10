package com.forme.shop.config;

import com.forme.shop.config.jwt.JwtAuthEntryPoint;
import com.forme.shop.config.jwt.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

                // URL 별 접근 권한 설정 (순서 중요 - 구체적인 것부터 작성)
                .authorizeHttpRequests(auth -> auth
                        // 회원가입, 로그인은 누구나 접근 가능
                        .requestMatchers("/api/register", "/api/login").permitAll()
                        // 상품 조회는 비로그인도 가능
                        .requestMatchers("/api/products/**").permitAll()
                        // 카테고리 조회는 비로그인도 가능
                        .requestMatchers("/api/categories/**").permitAll()
                        // 게시판 조회는 비로그인도 가능
                        .requestMatchers("/api/boards/**").permitAll()
                        // 댓글 조회는 비로그인도 가능
                        .requestMatchers("/api/comments/**").permitAll()
                        // Q&A 조회는 비로그인도 가능
                        .requestMatchers("/api/qna/**").permitAll()
                        // 관리자만 접근 가능
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        // 그 외 /api/** 는 로그인 필요 (장바구니, 주문, 마이페이지 등)
                        .requestMatchers("/api/**").authenticated()
                        // 나머지(SPA 정적 리소스, Vue Router 경로) 는 누구나 접근 가능
                        // → "/", "/index.html", "/assets/**", "/products/123" 같은 SPA 라우트 모두 통과
                        .anyRequest().permitAll()
                )

                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                // 모든 요청에서 JWT 토큰을 먼저 검증
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}