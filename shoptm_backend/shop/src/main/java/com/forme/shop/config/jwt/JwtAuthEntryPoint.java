package com.forme.shop.config.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    // 인증되지 않은 사용자가 보호된 API에 접근할 때 호출됨
    // 401 Unauthorized 응답 반환
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);       // 401
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": \"로그인이 필요합니다.\"}");
    }
}