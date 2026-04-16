package com.forme.shop.size.controller;

import com.forme.shop.size.dto.SizeRequest;
import com.forme.shop.size.dto.SizeResponse;
import com.forme.shop.size.service.SizeRecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/size")
@RequiredArgsConstructor
public class SizeRecommendController {

    private final SizeRecommendService sizeRecommendService;

    @PostMapping("/recommend")
    public ResponseEntity<?> recommend(@RequestBody SizeRequest request) {
        try {
            if (request.getHeight() == null || request.getWeight() == null)
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "키와 몸무게를 입력해주세요."));
            if (request.getHeight() < 100 || request.getHeight() > 250)
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "키는 100~250cm 범위로 입력해주세요."));
            if (request.getWeight() < 30 || request.getWeight() > 200)
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "몸무게는 30~200kg 범위로 입력해주세요."));

            SizeResponse res = sizeRecommendService.recommend(request);
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("success", true);
            body.put("recommendedSize", res.getRecommendedSize());
            body.put("krSize", res.getKrSize());
            body.put("brandSize", res.getBrandSize());
            body.put("brandCountry", res.getBrandCountry());
            body.put("message", res.getMessage());
            body.put("sizeChart", res.getSizeChart());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
