package com.forme.shop.settings.controller;

import com.forme.shop.settings.service.SiteSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SiteSettingController {

    private final SiteSettingService service;

    // 설정 조회 (공개 — 프론트에서 히어로, 매거진 등 로드)
    @GetMapping("/settings/{key}")
    public ResponseEntity<Map<String, String>> getSetting(@PathVariable String key) {
        return service.getValue(key)
                .map(v -> ResponseEntity.ok(Map.of("key", key, "value", v)))
                .orElse(ResponseEntity.ok(Map.of("key", key, "value", "")));
    }

    // 설정 저장 (관리자 전용)
    @PutMapping("/admin/settings/{key}")
    public ResponseEntity<Void> setSetting(@PathVariable String key,
                                           @RequestBody Map<String, String> body) {
        service.setValue(key, body.get("value"));
        return ResponseEntity.ok().build();
    }
}
