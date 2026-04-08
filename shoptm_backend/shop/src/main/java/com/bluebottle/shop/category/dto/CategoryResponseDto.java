package com.bluebottle.shop.category.dto;

import com.bluebottle.shop.category.entity.Category;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

// 클라이언트에게 카테고리 정보를 응답할 때 사용하는 DTO
@Getter
@Builder
public class CategoryResponseDto {

    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;

    // Category 엔티티를 CategoryResponseDto 로 변환하는 정적 메서드
    public static CategoryResponseDto from(Category category) {
        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .build();
    }
}