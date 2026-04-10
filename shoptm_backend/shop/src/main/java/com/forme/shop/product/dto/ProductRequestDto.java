package com.forme.shop.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

public class ProductRequestDto {

    // 상품 등록 요청 DTO (관리자)
    @Getter @Setter
    public static class Create {

        @NotBlank(message = "상품명을 입력해주세요.")
        private String name;

        private String description;  // 선택 입력

        @NotNull(message = "카테고리를 선택해주세요.")
        private Long categoryId;       // 카테고리 ID 추가

        @NotNull(message = "가격을 입력해주세요.")
        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        private Integer price;

        @NotNull(message = "재고를 입력해주세요.")
        @Min(value = 0, message = "재고는 0개 이상이어야 합니다.")
        private Integer stock;

        private String category;     // 선택 입력

        private Boolean isNew       = false;  // 신상품 여부
        private Boolean isBest      = false;  // 베스트 여부
        private Boolean isRecommend = false;  // 추천 여부
    }

    // 상품 수정 요청 DTO (관리자)
    // 수정할 항목만 보내면 되므로 전부 선택 입력
    @Getter @Setter
    public static class Update {
        private Long categoryId;       // 카테고리 변경 시 사용
        private String name;
        private String description;
        private Integer price;
        private Integer stock;
        private String category;
        private String status;       // ON_SALE / SOLD_OUT / HIDDEN
        private Boolean isNew;
        private Boolean isBest;
        private Boolean isRecommend;
    }
}