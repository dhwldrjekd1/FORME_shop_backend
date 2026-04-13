package com.forme.shop.size.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SizeRequest {
    private Double height;
    private Double weight;
    private String fit;       // slim, standard, wide
    private String gender;    // 남성, 여성
    private String brand;     // BEANPOLE, CARHARTT, LEVI'S, DICKIES
    private String category;  // top, bottom
}
