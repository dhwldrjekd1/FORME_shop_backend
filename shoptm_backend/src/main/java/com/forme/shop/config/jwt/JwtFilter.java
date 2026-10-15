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
                // 토큰 발급 시점의 role을 그대로 믿는다 — DB를 매 요청마다 다시 조회하지 않음
                // (정지/탈퇴는 위 revokeAllForEmail 기반 블랙리스트로 별도 처리하므로 여기 role과는
                // 무관). 지금은 이게 안전한 이유: 회원의 role을 바꾸는 기능 자체가 코드에 없음
                // (MemberService에 changeGrade는 있어도 changeRole/setRole은 없음) — 그래서 이
                // 토큰의 role이 발급 후에 실제로 달라질 수 있는 경로가 아예 없다. 나중에 "관리자
                // 권한 부여/회수" 같은 기능이 추가되면, 그 메서드도 MemberService.banMember()/
                // withdraw()가 하듯 tokenBlacklistService.revokeAllForEmail(email)을 반드시 함께
                // 호출해야 한다 — 안 그러면 강등된 관리자가 기존 토큰 만료 시각(최대 24시간)까지
                // 계속 ROLE_ADMIN으로 행세할 수 있다(정지/탈퇴 때 이미 한 번 겪었던 것과 같은
                // 유형의 버그가 재발하는 것 — 교차검증에서 "확인 필요"로 지적됨).
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