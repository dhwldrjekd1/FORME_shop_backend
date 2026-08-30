package com.forme.shop.faq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

// DTO(Data Transfer Object): 클라이언트 ↔ 서버 간 데이터 전달 전용 객체
public class FaqRequestDto {

    // FAQ 등록 요청 DTO (관리자)
    @Getter @Setter
    public static class Create {

        @NotBlank(message = "카테고리를 선택해주세요.")
        private String category;       // 주문/배송/반품/결제/상품/계정

        @NotBlank(message = "질문을 입력해주세요.")
        private String question;

        @NotBlank(message = "답변을 입력해주세요.")
        private String answer;

        private Integer sortOrder;     // 정렬 순서 (선택, 기본값 0)
    }

    // FAQ 수정 요청 DTO (관리자)
    // 수정할 항목만 보내면 되므로 선택 입력 — 세 필드 다 null 허용이면서도(안 보내면 그대로
    // 유지) "보냈는데 공백뿐임"은 걸러야 해서 @NotBlank(null 자체를 막음) 대신 @Pattern을 쓴다
    // (MemberRequestDto.Update.name과 동일한 이유·동일한 정규식)
    @Getter @Setter
    public static class Update {
        @Pattern(regexp = "(?sU).*\\S.*", message = "카테고리를 선택해주세요.")
        private String category;
        @Pattern(regexp = "(?sU).*\\S.*", message = "질문을 입력해주세요.")
        private String question;
        @Pattern(regexp = "(?sU).*\\S.*", message = "답변을 입력해주세요.")
        private String answer;
        private Integer sortOrder;
    }
}
