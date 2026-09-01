package com.forme.shop.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

public class ProductRequestDto {

    // 사이즈별 재고 DTO
    @Getter @Setter
    public static class SizeStock {
        private String size;    // XS, S, M, L, XL, XXL, 28, 30 등

        // 사이즈별 재고 합계가 상품 전체 stock으로 그대로 대체되므로(ProductService 참고),
        // 여기를 검증하지 않으면 Create/Update의 stock @Min(0)을 그대로 우회해 음수 재고가
        // 저장될 수 있었음.
        @Min(value = 0, message = "재고는 0개 이상이어야 합니다.")
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

        // 서버는 이 값을 읽지 않고 categoryId만 쓰지만, 프론트(AdminProducts.vue)가 여전히
        // 이 키를 요청 바디에 실어 보낸다 — Jackson의 FAIL_ON_UNKNOWN_PROPERTIES가 기본값
        // (Spring Boot도 별도 설정 없이는 끄지 않음)이라, 필드를 지우면 모르는 JSON 필드로
        // 걸려 상품 등록/수정 요청 자체가 400으로 전부 실패한다. 실제로 쓰이진 않아도 지우면
        // 안 되는 필드(죽은 코드 정리 중 교차검증에서 발견).
        private String category;     // 선택 입력 (미사용 — categoryId로 대체됨)
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
        @Valid
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

        @Pattern(regexp = "(?sU).*\\S.*", message = "상품명을 입력해주세요.")
        private String name;

        private String description;

        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        private Integer price;

        @Min(value = 0, message = "재고는 0개 이상이어야 합니다.")
        private Integer stock;

        // Create.category와 동일한 이유로 유지 — 프론트가 수정 요청에도 이 키를 그대로 실어
        // 보내므로, 미사용이어도 지우면 Jackson이 모르는 필드로 걸려 요청 전체가 400으로 실패함.
        private String category;     // 선택 입력 (미사용 — categoryId로 대체됨)
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
        @Valid
        private java.util.List<SizeStock> sizeStocks;
        private Boolean isNew;
        private Boolean isBest;
    }
}