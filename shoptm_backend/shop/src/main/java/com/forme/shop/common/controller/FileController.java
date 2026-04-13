package com.forme.shop.common.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/admin/files")
public class FileController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    // 서버 이미지 전체 목록 (uploads + public/new + public/images)
    @GetMapping
    public ResponseEntity<?> listFiles(@RequestParam(defaultValue = "") String brand) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 1) uploads 폴더
        scanFolder(new File(uploadDir), "/uploads/", "업로드", result);

        // 2) public/new 폴더
        String[] newPaths = {"/app/static-new", "../public/new", "src/main/resources/static/new", "public/new"};
        for (String base : newPaths) {
            File dir = new File(base);
            if (dir.exists() && dir.isDirectory()) {
                scanRecursive(dir, "/new/", "신규", result, brand);
                break;
            }
        }

        // 3) public/images 폴더 (기존 이미지)
        String[] imgPaths = {"/app/static-images", "../public/images", "src/main/resources/static/images", "public/images"};
        for (String base : imgPaths) {
            File dir = new File(base);
            if (dir.exists() && dir.isDirectory()) {
                scanRecursive(dir, "/images/", "기존", result, brand);
                break;
            }
        }

        // 브랜드 필터
        if (brand != null && !brand.isBlank()) {
            String lower = brand.toLowerCase();
            result = result.stream()
                    .filter(m -> {
                        String url = (String) m.get("url");
                        String group = (String) m.get("group");
                        return url.toLowerCase().contains(lower) || "업로드".equals(group);
                    })
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(result);
    }

    private void scanFolder(File dir, String urlPrefix, String group, List<Map<String, Object>> result) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles((d, name) -> isImage(name));
        if (files == null) return;
        for (File f : files) {
            result.add(makeEntry(f.getName(), urlPrefix + f.getName(), group, f));
        }
    }

    private void scanRecursive(File dir, String urlPrefix, String groupPrefix, List<Map<String, Object>> result, String brandFilter) {
        if (dir == null || !dir.exists()) return;
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            walk.filter(p -> Files.isRegularFile(p) && isImage(p.getFileName().toString()))
                .forEach(p -> {
                    // 상대 경로 계산
                    String relative = dir.toPath().relativize(p).toString().replace('\\', '/');
                    String url = urlPrefix + relative;

                    // 그룹 = 브랜드 폴더명 추출
                    String[] parts = relative.split("/");
                    String group = parts.length > 1 ? groupPrefix + " · " + parts[0].toUpperCase() : groupPrefix;

                    result.add(makeEntry(p.getFileName().toString(), url, group, p.toFile()));
                });
        } catch (Exception ignored) {}
    }

    private Map<String, Object> makeEntry(String name, String url, String group, File f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("url", url);
        m.put("group", group);
        m.put("size", f.length());
        return m;
    }

    private boolean isImage(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".gif")
                || lower.endsWith(".webp");
    }
}
