package com.forme.shop.size.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SizeRequest {
    private Double height;
    private Double weight;
    private String fit;       // slim, regular, loose
    private String category;  // top, bottom, outer
}
