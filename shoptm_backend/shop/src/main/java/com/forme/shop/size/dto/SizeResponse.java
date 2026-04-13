package com.forme.shop.size.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SizeResponse {
    private String recommendedSize;
    private String message;
    private String krSize;           // 한국 사이즈
    private String brandSize;        // 브랜드 국가 사이즈
    private String brandCountry;     // US/UK/EU/KR
    private List<Map<String, String>> sizeChart;

    public SizeResponse() {}

    public SizeResponse(String recommendedSize, String message, List<Map<String, String>> sizeChart) {
        this.recommendedSize = recommendedSize;
        this.message = message;
        this.sizeChart = sizeChart;
    }
}
