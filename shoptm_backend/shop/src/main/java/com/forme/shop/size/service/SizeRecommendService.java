package com.forme.shop.size.service;

import com.forme.shop.size.dto.SizeRequest;
import com.forme.shop.size.dto.SizeResponse;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SizeRecommendService {

    public SizeResponse recommend(SizeRequest request) {
        String category = request.getCategory() != null ? request.getCategory() : "top";
        String fit = request.getFit() != null ? request.getFit() : "regular";
        double bmi = request.getWeight() / Math.pow(request.getHeight() / 100.0, 2);

        String baseSize = determineBaseSize(request.getHeight(), request.getWeight(), category);
        String recommended = adjustByFit(baseSize, fit);

        String fitKor = switch (fit) { case "slim" -> "슬림핏"; case "loose" -> "루즈핏"; default -> "레귤러핏"; };
        String message = String.format("%s 선호에 맞춰 %s 사이즈를 추천드립니다.", fitKor, recommended);

        return new SizeResponse(recommended, message, buildSizeChart(category));
    }

    private String determineBaseSize(double height, double weight, String category) {
        double bmi = weight / Math.pow(height / 100.0, 2);
        if (category.equals("bottom")) {
            if (weight < 55) return "S"; if (weight < 65) return "M";
            if (weight < 75) return "L"; if (weight < 85) return "XL"; return "XXL";
        }
        if (height < 165) {
            if (bmi < 20) return "S"; if (bmi < 24) return "M"; if (bmi < 27) return "L"; return "XL";
        } else if (height < 175) {
            if (bmi < 20) return "M"; if (bmi < 24) return "L"; if (bmi < 27) return "XL"; return "XXL";
        } else {
            if (bmi < 20) return "M"; if (bmi < 23) return "L"; if (bmi < 26) return "XL"; return "XXL";
        }
    }

    private String adjustByFit(String baseSize, String fit) {
        String[] sizes = {"XS", "S", "M", "L", "XL", "XXL"};
        int idx = -1;
        for (int i = 0; i < sizes.length; i++) if (sizes[i].equals(baseSize)) { idx = i; break; }
        if (idx == -1) return baseSize;
        return switch (fit) {
            case "slim" -> sizes[Math.max(0, idx - 1)];
            case "loose" -> sizes[Math.min(sizes.length - 1, idx + 1)];
            default -> baseSize;
        };
    }

    private List<Map<String, String>> buildSizeChart(String category) {
        List<Map<String, String>> chart = new ArrayList<>();
        if (category.equals("bottom")) {
            chart.add(Map.of("size","S","col1","26~27","col2","68~71","col3","93~96"));
            chart.add(Map.of("size","M","col1","28~29","col2","72~75","col3","97~100"));
            chart.add(Map.of("size","L","col1","30~31","col2","76~79","col3","101~104"));
            chart.add(Map.of("size","XL","col1","32~33","col2","80~83","col3","105~108"));
            chart.add(Map.of("size","XXL","col1","34~36","col2","84~88","col3","109~113"));
        } else {
            chart.add(Map.of("size","S","col1","어깨 43","col2","가슴 96","col3","총장 66"));
            chart.add(Map.of("size","M","col1","어깨 45","col2","가슴 100","col3","총장 68"));
            chart.add(Map.of("size","L","col1","어깨 47","col2","가슴 104","col3","총장 70"));
            chart.add(Map.of("size","XL","col1","어깨 49","col2","가슴 108","col3","총장 72"));
            chart.add(Map.of("size","XXL","col1","어깨 51","col2","가슴 112","col3","총장 74"));
        }
        return chart;
    }
}
