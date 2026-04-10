package com.forme.shop.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

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

        // ─── Vue SPA 정적 리소스 + history mode 폴백 ───
        // /** 로 들어오는 모든 요청에 대해:
        //   1) classpath:/static/ 에 실제 파일이 있으면 그대로 서빙 (assets/*.js, *.css, favicon.ico 등)
        //   2) 없으면 index.html 로 폴백 → Vue Router 가 클라이언트 사이드에서 라우팅
        // /api/** 요청은 @RestController 가 더 우선이라 이 핸들러로 안 옴.
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location)
                            throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;     // 실제 파일 (JS/CSS/이미지 등)
                        }
                        // 파일 없으면 SPA 진입점으로 폴백 (Vue Router 가 처리)
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}