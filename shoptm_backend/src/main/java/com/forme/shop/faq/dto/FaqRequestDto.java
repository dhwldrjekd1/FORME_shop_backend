package com.forme.shop.faq.dto;

import jakarta.validation.constraints.NotBlank;
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
    // 수정할 항목만 보내면 되므로 선택 입력
    @Getter @Setter
    public static class Update {
        private String category;
        private String question;
        private String answer;
        private Integer sortOrder;
    }
}
