package com.forme.shop.common.security;

import com.forme.shop.config.jwt.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// 로그인한 사용자 본인(또는 관리자)인지 확인하는 공통 유틸
// - JwtFilter가 SecurityContext에 이메일(JWT subject)을 principal로 등록해둠
// - 컨트롤러가 아니라 서비스 계층에서 검증해야, 어떤 경로로 호출되든 우회할 수 없음
public class SecurityUtil {

    private SecurityUtil() {}

    // 현재 로그인한 사용자의 이메일 (JWT subject)
    public static String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    // 현재 로그인한 사용자가 관리자(ROLE_ADMIN)인지
    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // 요청에서 JWT를 꺼낸다 — Authorization 헤더 우선, 없으면 httpOnly 쿠키에서 찾음
    // (브라우저는 쿠키로 보내고, curl/Postman 같은 도구는 헤더로 보내는 것 둘 다 지원)
    public static String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (JwtUtil.COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // 리소스 소유자(email)가 본인이 아니고 관리자도 아니면 403
    // 예: 다른 회원의 주문/장바구니/회원정보에 접근하려는 요청을 차단
    public static void checkOwnerOrAdmin(String resourceOwnerEmail) {
        if (isAdmin()) return;
        String current = getCurrentEmail();
        if (current == null || !current.equals(resourceOwnerEmail)) {
            throw new AccessDeniedException("본인의 정보만 접근할 수 있습니다.");
        }
    }
}
