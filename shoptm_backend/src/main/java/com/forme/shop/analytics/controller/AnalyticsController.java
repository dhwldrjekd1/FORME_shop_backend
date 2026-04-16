package com.forme.shop.analytics.controller;

import com.forme.shop.analytics.dto.PageViewRequest;
import com.forme.shop.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

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
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
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
