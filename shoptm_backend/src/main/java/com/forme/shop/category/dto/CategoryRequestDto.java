package com.forme.shop.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    // 수정할 항목만 보내면 되므로 선택 입력 — name은 null 허용이면서도(안 보내면 그대로 유지)
    // "보냈는데 공백뿐임"은 걸러야 해서 @NotBlank(null 자체를 막음) 대신 @Pattern을 쓴다
    // (MemberRequestDto.Update.name과 동일한 이유·동일한 정규식)
    @Getter @Setter
    public static class Update {
        @Pattern(regexp = "(?sU).*\\S.*", message = "카테고리명을 입력해주세요.")
        private String name;
        private String description;
        private Integer sortOrder;
        private Boolean isActive;      // 활성/비활성 변경
    }
}