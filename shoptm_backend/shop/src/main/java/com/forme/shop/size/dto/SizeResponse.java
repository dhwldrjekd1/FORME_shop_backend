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
    private List<Map<String, String>> sizeChart;

    public SizeResponse() {}

    public SizeResponse(String recommendedSize, String message, List<Map<String, String>> sizeChart) {
        this.recommendedSize = recommendedSize;
        this.message = message;
        this.sizeChart = sizeChart;
    }
}
