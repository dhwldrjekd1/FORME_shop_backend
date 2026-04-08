package com.bluebottle.shop.qna.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// DTO(Data Transfer Object): 클라이언트 ↔ 서버 간 데이터 전달 전용 객체
public class QnaRequestDto {

    // 질문 등록 요청 DTO (일반회원)
    @Getter @Setter
    public static class Create {

        private Long productId;        // 질문 대상 상품 ID (선택)

        @NotBlank(message = "제목을 입력해주세요.")
        private String title;          // 질문 제목

        @NotBlank(message = "내용을 입력해주세요.")
        private String content;        // 질문 내용

        private Boolean isSecret = false;  // 비밀글 여부 (기본값 false)
    }

    // 질문 수정 요청 DTO (일반회원)
    // 수정할 항목만 보내면 되므로 선택 입력
    @Getter @Setter
    public static class Update {
        private String title;          // 수정할 제목 (선택)
        private String content;        // 수정할 내용 (선택)
        private Boolean isSecret;      // 비밀글 여부 변경 (선택)
    }

    // 답변 등록/수정 요청 DTO (관리자)
    @Getter @Setter
    public static class Answer {

        @NotBlank(message = "답변 내용을 입력해주세요.")
        private String answer;         // 답변 내용
    }
}