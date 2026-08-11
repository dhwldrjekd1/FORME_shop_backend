package com.forme.shop.config.jwt;

import com.forme.shop.common.security.SecurityUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Authorization 헤더 또는 httpOnly 쿠키(auth_token)에서 토큰 추출
        String token = SecurityUtil.extractToken(request);

        if (token != null && jwtUtil.validateToken(token)) {
            String email = jwtUtil.getEmail(token);  // 토큰에서 이메일 추출

            // 서명·만료는 유효하더라도 인증 처리하지 않는 두 경우:
            // ① 로그아웃 등으로 이 토큰(jti)만 콕 집어 폐기된 경우
            // ② 이 토큰을 가진 회원이 그 뒤에 정지/탈퇴돼서, 발급 시각이 그 회원의
            //    전체 무효화 기준 시각보다 이전인 경우 (관리자가 강퇴해도 최대 24시간
            //    동안 계속 로그인 상태로 남아있던 문제의 수정)
            boolean valid = !tokenBlacklistService.isRevoked(jwtUtil.getJti(token))
                    && !tokenBlacklistService.isRevokedForEmail(email, jwtUtil.getIssuedAt(token).toInstant());
            if (valid) {
                String role = jwtUtil.getRole(token);   // 토큰에서 권한 추출

                // Spring Security에 인증 정보 등록
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority(role))  // 권한 설정
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}