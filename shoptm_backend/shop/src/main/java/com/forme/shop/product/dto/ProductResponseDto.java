package com.forme.shop.product.dto;

import com.forme.shop.product.entity.Product;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

// 클라이언트에게 상품 정보를 응답할 때 사용하는 DTO
// Entity 를 직접 반환하지 않고 DTO 로 변환해서 필요한 정보만 전달
@Getter
@Builder
public class ProductResponseDto {

    private Long id;
    private Long categoryId;       // 카테고리 ID
    private String categoryName;   // 카테고리명
    private String name;           // 상품명
    private String description;    // 상품 설명
    private Integer price;         // BigDecimal → Integer
    private Integer stock;         // 재고 수량
    private String imageUrl;       // 이미지 경로
    private Boolean isNew;         // 신상품 여부
    private Boolean isBest;        // 베스트 여부
    private Boolean isRecommend;   // 추천 여부
    private Boolean isActive;      // 활성 여부
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Product 엔티티를 ProductResponseDto 로 변환하는 정적 메서드
    public static ProductResponseDto from(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                // 카테고리가 있을 때만 ID 와 이름 반환
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .isNew(product.getIsNew())
                .isBest(product.getIsBest())
                .isRecommend(product.getIsRecommend())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}