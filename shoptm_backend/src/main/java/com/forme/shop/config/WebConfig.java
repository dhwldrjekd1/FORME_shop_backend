package com.forme.shop.config;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration  // 스프링 설정 클래스임을 선언
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    // CORS 설정
    // Vue.js (프론트엔드) 에서 Spring Boot (백엔드) API 호출 시 필요
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")          // /api/** 로 시작하는 모든 URL 에 CORS 적용
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "https://forme.dyy.kr"
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
        // uploadDir 경로에 저장된 이미지를 /uploads/** URL로 서빙
        String resolvedPath = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        if (!resolvedPath.startsWith("file:")) resolvedPath = "file:" + resolvedPath;
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resolvedPath);

        // Vite 빌드 산출물(assets/*.js, *.css): 파일명에 컨텐츠 해시가 포함되어 있어
        // 내용이 바뀌면 파일명도 함께 바뀜 → 브라우저가 영구 캐싱해도 안전 (배포 = 새 URL)
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        // hero/상품 이미지 등: 파일명에 해시가 없어 파일을 교체해도 URL이 그대로라
        // 너무 길게 캐싱하면 교체가 안 보일 수 있음 → 하루 정도로 절충
        // 주의: URL 패턴의 매칭된 접두어(/new/, /images/)는 벗겨진 뒤 나머지 경로로
        // resourceLocations 안에서 찾으므로, location 에도 같은 접두어를 붙여줘야 함.
        CacheControl dailyCache = CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic();
        registry.addResourceHandler("/new/**")
                .addResourceLocations("classpath:/static/new/")
                .setCacheControl(dailyCache);
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCacheControl(dailyCache);
        registry.addResourceHandler("/main1.jpg", "/main2.jpg", "/main3.jpg", "/main4.jpg")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(dailyCache);

        // ─── Vue SPA 정적 리소스 + history mode 폴백 ───
        // /** 로 들어오는 모든 요청에 대해:
        //   1) classpath:/static/ 에 실제 파일이 있으면 그대로 서빙 (위 핸들러에 안 걸린 파일들)
        //   2) 없으면 index.html 로 폴백 → Vue Router 가 클라이언트 사이드에서 라우팅
        // index.html은 배포 때마다 최신 번들 파일명을 가리켜야 하므로 캐싱하지 않고 매번 재검증.
        // /api/** 요청은 @RestController 가 더 우선이라 이 핸들러로 안 옴.
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
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