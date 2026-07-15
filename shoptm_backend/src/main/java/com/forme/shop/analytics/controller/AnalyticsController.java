package com.forme.shop.analytics.controller;

import com.forme.shop.analytics.dto.PageViewRequest;
import com.forme.shop.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;

    @PostMapping("/track")
    public ResponseEntity<?> track(@RequestBody PageViewRequest request) {
        try {
            if (request.getPageName() == null || request.getDuration() == null)
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "필수값 누락"));
            if (request.getDuration() < 2)
                return ResponseEntity.ok(Map.of("success", true, "message", "ignored"));
            analyticsService.record(request);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("페이지뷰 기록 실패", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "요청 처리에 실패했습니다."));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary() {
        return ResponseEntity.ok(analyticsService.getSummary());
    }

    @GetMapping("/pages")
    public ResponseEntity<?> pageStats() {
        return ResponseEntity.ok(analyticsService.getPageStats());
    }

    @GetMapping("/users")
    public ResponseEntity<?> userStats() {
        return ResponseEntity.ok(analyticsService.getUserStats());
    }

    @GetMapping("/hourly")
    public ResponseEntity<?> hourlyStats() {
        return ResponseEntity.ok(analyticsService.getHourlyStats());
    }

    @GetMapping("/recent")
    public ResponseEntity<?> recentViews() {
        return ResponseEntity.ok(analyticsService.getRecentViews());
    }

    @GetMapping("/products")
    public ResponseEntity<?> productDetailStats() {
        return ResponseEntity.ok(analyticsService.getProductDetailStats());
    }
}
