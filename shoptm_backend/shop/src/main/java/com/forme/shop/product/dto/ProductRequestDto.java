package com.forme.shop.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

public class ProductRequestDto {

    // 사이즈별 재고 DTO
    @Getter @Setter
    public static class SizeStock {
        private String size;    // XS, S, M, L, XL, XXL, 28, 30 등
        private Integer stock;  // 해당 사이즈 재고
    }


    // 상품 등록 요청 DTO (관리자)
    @Getter @Setter
    public static class Create {

        private Long id;             // 상품 ID (직접 지정, null이면 자동생성)

        @NotBlank(message = "상품명을 입력해주세요.")
        private String name;

        private String description;  // 선택 입력

        private Long categoryId;       // 카테고리 ID (null이면 기본 카테고리 사용)

        @NotNull(message = "가격을 입력해주세요.")
        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        private Integer price;

        @NotNull(message = "재고를 입력해주세요.")
        @Min(value = 0, message = "재고는 0개 이상이어야 합니다.")
        private Integer stock;

        private String category;     // 선택 입력
        private String size;         // 사이즈 (S, M, L, XL, FREE 등)
        private String gender;       // 성별 (남성, 여성, 공용)
        private String brand;        // 브랜드 (BEANPOLE, CARHARTT 등)
        private Integer discountRate;   // 할인율 (%)
        private Integer originalPrice;  // 할인 전 가격
        private String imageUrl;        // 서버 이미지 URL (직접 지정)
        private String imageUrls;       // 서버 다중 이미지 URL (콤마 구분)
        private String thumbnailUrl;    // 썸네일 이미지 URL
        private String curatorImageUrl; // 큐레이터 노출 이미지 URL (등록 시 직접 지정)
        private String colorName;
        private String colorHex;
        private String features;        // 줄바꿈 구분
        private String composition;     // 줄바꿈 구분

        // 사이즈별 재고 [{ "size": "M", "stock": 10 }, ...]
        private java.util.List<SizeStock> sizeStocks;

        private Boolean isNew       = false;  // 신상품 여부
        private Boolean isBest      = false;  // 베스트 여부
        private Boolean isRecommend = false;  // 추천 여부
    }

    // 상품 수정 요청 DTO (관리자)
    // 수정할 항목만 보내면 되므로 전부 선택 입력
    // 추천(큐레이터)은 전용 API로 분리: PATCH /admin/products/{id}/recommend
    @Getter @Setter
    public static class Update {
        private Long categoryId;
        private String name;
        private String description;
        private Integer price;
        private Integer stock;
        private String category;
        private String size;
        private String gender;
        private String brand;
        private Integer discountRate;
        private Integer originalPrice;
        private String imageUrl;
        private String imageUrls;
        private String thumbnailUrl;
        private String colorName;
        private String colorHex;
        private String features;
        private String composition;
        private java.util.List<SizeStock> sizeStocks;
        private String status;
        private Boolean isNew;
        private Boolean isBest;
    }
}