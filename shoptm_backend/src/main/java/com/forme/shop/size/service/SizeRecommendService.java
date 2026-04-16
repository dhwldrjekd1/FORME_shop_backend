package com.forme.shop.size.service;

import com.forme.shop.size.dto.SizeRequest;
import com.forme.shop.size.dto.SizeResponse;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SizeRecommendService {

    // ── 남성 상의 ──
    private static final List<Map<String, String>> MALE_TOP = List.of(
        Map.of("size","XS", "US","XS",    "UK","XS",    "EU","44",    "KR","90"),
        Map.of("size","S",  "US","S",     "UK","S",     "EU","46",    "KR","95"),
        Map.of("size","M",  "US","M",     "UK","M",     "EU","48-50", "KR","100"),
        Map.of("size","L",  "US","L",     "UK","L",     "EU","52",    "KR","105"),
        Map.of("size","XL", "US","XL",    "UK","XL",    "EU","54-56", "KR","110")
    );

    // ── 여성 상의 ──
    private static final List<Map<String, String>> FEMALE_TOP = List.of(
        Map.of("size","XS", "US","0-2",   "UK","4-6",   "EU","32-34", "KR","44"),
        Map.of("size","S",  "US","4-6",   "UK","8-10",  "EU","36",    "KR","55"),
        Map.of("size","M",  "US","6-8",   "UK","10-12", "EU","38-40", "KR","66"),
        Map.of("size","L",  "US","8-10",  "UK","12-14", "EU","40-42", "KR","77"),
        Map.of("size","XL", "US","12-14", "UK","16-18", "EU","42-44", "KR","88")
    );

    // ── 남성 하의/데님 ──
    private static final List<Map<String, String>> MALE_BOTTOM = List.of(
        Map.of("size","28", "US","28", "UK","28", "EU","44", "KR","28"),
        Map.of("size","30", "US","30", "UK","30", "EU","46", "KR","30"),
        Map.of("size","32", "US","32", "UK","32", "EU","48", "KR","32"),
        Map.of("size","34", "US","34", "UK","34", "EU","50", "KR","34"),
        Map.of("size","36", "US","36", "UK","36", "EU","52", "KR","36")
    );

    // ── 여성 하의/데님 ──
    private static final List<Map<String, String>> FEMALE_BOTTOM = List.of(
        Map.of("size","XS",  "US","0",  "UK","4",  "EU","32", "KR","44"),
        Map.of("size","S",   "US","2",  "UK","6",  "EU","34", "KR","44"),
        Map.of("size","M",   "US","4",  "UK","8",  "EU","36", "KR","55"),
        Map.of("size","L",   "US","6",  "UK","10", "EU","38", "KR","66"),
        Map.of("size","XL",  "US","8",  "UK","12", "EU","40", "KR","66"),
        Map.of("size","XXL", "US","10", "UK","14", "EU","42", "KR","77"),
        Map.of("size","3XL", "US","12", "UK","16", "EU","44", "KR","88")
    );

    // 브랜드 → 국가
    private static final Map<String, String> BRAND_COUNTRY = Map.of(
        "BEANPOLE", "KR",
        "DICKIES",  "US",
        "CARHARTT", "UK",
        "LEVI'S",   "EU"
    );

    public SizeResponse recommend(SizeRequest request) {
        String category = request.getCategory() != null ? request.getCategory() : "top";
        String fit = request.getFit() != null ? request.getFit() : "standard";
        String gender = request.getGender() != null ? request.getGender() : "남성";
        String brand = request.getBrand() != null ? request.getBrand().toUpperCase() : "";

        // 기본 사이즈 결정
        String baseSize = determineBaseSize(request.getHeight(), request.getWeight(), category, gender);
        String recommended = adjustByFit(baseSize, fit, category, gender);

        String country = BRAND_COUNTRY.getOrDefault(brand, "KR");
        List<Map<String, String>> chart = getChart(category, gender);

        String krSize = findValue(recommended, chart, "KR");
        String brandSize = findValue(recommended, chart, country);

        String fitKor = switch (fit) { case "slim" -> "슬림"; case "wide" -> "와이드"; default -> "스탠다드"; };
        String message;
        if ("KR".equals(country)) {
            message = String.format("%s 핏 기준, %s 사이즈(한국 %s)를 추천드립니다.", fitKor, recommended, krSize);
        } else {
            String countryName = switch (country) { case "US" -> "미국"; case "UK" -> "영국"; case "EU" -> "유럽"; default -> "한국"; };
            message = String.format("%s 핏 기준, %s(%s) 사이즈 → 한국 사이즈 %s를 추천드립니다.", fitKor, brandSize, countryName, krSize);
        }

        List<Map<String, String>> responseChart = new ArrayList<>();
        for (Map<String, String> row : chart) {
            Map<String, String> r = new LinkedHashMap<>();
            r.put("size", row.get("size"));
            r.put("US", row.get("US"));
            r.put("UK", row.get("UK"));
            r.put("EU", row.get("EU"));
            r.put("KR", row.get("KR"));
            responseChart.add(r);
        }

        SizeResponse response = new SizeResponse(recommended, message, responseChart);
        response.setKrSize(krSize);
        response.setBrandSize(brandSize);
        response.setBrandCountry(country);
        return response;
    }

    private List<Map<String, String>> getChart(String category, String gender) {
        boolean isBottom = category.equals("bottom");
        boolean isFemale = gender.equals("여성");
        if (isBottom) return isFemale ? FEMALE_BOTTOM : MALE_BOTTOM;
        return isFemale ? FEMALE_TOP : MALE_TOP;
    }

    private String findValue(String size, List<Map<String, String>> chart, String col) {
        for (Map<String, String> row : chart) {
            if (row.get("size").equals(size)) return row.getOrDefault(col, size);
        }
        return size;
    }

    private String determineBaseSize(double height, double weight, String category, String gender) {
        double bmi = weight / Math.pow(height / 100.0, 2);

        if (category.equals("bottom")) {
            if (gender.equals("여성")) {
                if (weight < 45) return "XS"; if (weight < 50) return "S"; if (weight < 58) return "M";
                if (weight < 65) return "L"; if (weight < 72) return "XL"; if (weight < 80) return "XXL"; return "3XL";
            } else {
                if (weight < 58) return "28"; if (weight < 65) return "30"; if (weight < 73) return "32";
                if (weight < 82) return "34"; return "36";
            }
        }

        // 상의
        if (gender.equals("여성")) {
            if (bmi < 17.5) return "XS"; if (bmi < 20) return "S"; if (bmi < 23) return "M";
            if (bmi < 26) return "L"; return "XL";
        } else {
            if (height < 165) {
                if (bmi < 20) return "XS"; if (bmi < 22) return "S"; if (bmi < 25) return "M";
                if (bmi < 28) return "L"; return "XL";
            } else if (height < 175) {
                if (bmi < 19) return "S"; if (bmi < 23) return "M"; if (bmi < 26) return "L"; return "XL";
            } else {
                if (bmi < 20) return "M"; if (bmi < 23) return "L"; return "XL";
            }
        }
    }

    private String adjustByFit(String baseSize, String fit, String category, String gender) {
        List<String> sizes;
        if (category.equals("bottom") && !gender.equals("여성")) {
            sizes = List.of("28","30","32","34","36");
        } else if (category.equals("bottom") && gender.equals("여성")) {
            sizes = List.of("XS","S","M","L","XL","XXL","3XL");
        } else {
            sizes = List.of("XS","S","M","L","XL");
        }

        int idx = sizes.indexOf(baseSize);
        if (idx == -1) return baseSize;

        return switch (fit) {
            case "slim" -> sizes.get(Math.max(0, idx - 1));
            case "wide" -> sizes.get(Math.min(sizes.size() - 1, idx + 1));
            default -> baseSize;
        };
    }
}
