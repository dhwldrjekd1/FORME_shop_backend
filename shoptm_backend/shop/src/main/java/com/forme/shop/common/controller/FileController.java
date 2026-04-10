package com.forme.shop.common.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/files")
public class FileController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    // 서버에 업로드된 이미지 목록 반환
    @GetMapping
    public ResponseEntity<?> listFiles() {
        File dir = new File(uploadDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return ResponseEntity.ok(List.of());
        }

        File[] files = dir.listFiles((d, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".png") || lower.endsWith(".gif")
                    || lower.endsWith(".webp");
        });

        if (files == null) return ResponseEntity.ok(List.of());

        List<Map<String, Object>> result = Arrays.stream(files)
                .sorted((a, b) -> Long.compare(b.lastModified(), a.lastModified()))
                .map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", f.getName());
                    m.put("url", "/uploads/" + f.getName());
                    m.put("size", f.length());
                    m.put("modified", f.lastModified());
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // public 폴더의 이미지 목록 (브랜드 히어로 등)
    @GetMapping("/public")
    public ResponseEntity<?> listPublicImages(@RequestParam(defaultValue = "") String path) {
        // classpath:static/ 아래의 이미지 목록은 런타임에 확인이 어려우므로
        // 주요 경로를 하드코딩으로 제공
        List<Map<String, String>> publicImages = new ArrayList<>();

        // 히어로 이미지
        for (int i = 1; i <= 15; i++) {
            publicImages.add(Map.of("name", "hero_" + i + ".jpg", "url", "/hero/" + i + ".jpg", "group", "히어로"));
        }

        // 브랜드 메인 이미지
        publicImages.add(Map.of("name", "main1.jpg", "url", "/main1.jpg", "group", "메인"));
        publicImages.add(Map.of("name", "main2.jpg", "url", "/main2.jpg", "group", "메인"));
        publicImages.add(Map.of("name", "main3.jpg", "url", "/main3.jpg", "group", "메인"));
        publicImages.add(Map.of("name", "main4.jpg", "url", "/main4.jpg", "group", "메인"));

        return ResponseEntity.ok(publicImages);
    }
}
