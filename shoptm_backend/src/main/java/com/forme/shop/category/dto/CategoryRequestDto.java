package com.forme.shop.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

public class CategoryRequestDto {

    // 카테고리 등록 요청 DTO (관리자)
    @Getter @Setter
    public static class Create {

        @NotBlank(message = "카테고리명을 입력해주세요.")
        private String name;           // 카테고리명

        private String description;    // 설명 (선택)

        private Integer sortOrder = 0; // 정렬 순서 (기본값 0)
    }

    // 카테고리 수정 요청 DTO (관리자)
    // 수정할 항목만 보내면 되므로 선택 입력
    @Getter @Setter
    public static class Update {
        private String name;
        private String description;
        private Integer sortOrder;
        private Boolean isActive;      // 활성/비활성 변경
    }
}