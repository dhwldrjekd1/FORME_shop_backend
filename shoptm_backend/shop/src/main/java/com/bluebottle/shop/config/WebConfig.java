package com.bluebottle.shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration  // 스프링 설정 클래스임을 선언
public class WebConfig implements WebMvcConfigurer {

    // CORS 설정
    // Vue.js (프론트엔드) 에서 Spring Boot (백엔드) API 호출 시 필요
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")          // /api/** 로 시작하는 모든 URL 에 CORS 적용
                .allowedOrigins(
                        "http://localhost:5173", // Vue.js 기본 포트
                        "http://localhost:3000"  // 추가 포트 (필요 시)
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")             // 모든 헤더 허용
                .allowCredentials(true)          // 쿠키/인증 정보 허용
                .maxAge(3600);                   // 1시간 동안 preflight 캐시
    }

    // 이미지 업로드 파일 접근 설정
    // /uploads/** URL 로 서버에 저장된 이미지 파일 접근 가능하게 설정
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");  // 실제 파일 저장 경로
    }
}