package com.forme.shop.qna.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// DTO(Data Transfer Object): 클라이언트 ↔ 서버 간 데이터 전달 전용 객체
public class QnaRequestDto {

    // 질문 등록 요청 DTO (일반회원)
    @Getter @Setter
    public static class Create {

        private Long productId;        // 질문 대상 상품 ID (선택)

        // Qna.title 컬럼 길이(200)와 맞춤 — 없으면 DB가 거부하는
        // DataIntegrityViolationException이 그대로 500으로 노출됨(교차검증에서 발견).
        // content/answer는 TEXT 컬럼이라 DB 쪽 길이 제한이 없어 여기서는 다루지 않음.
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        private String title;          // 질문 제목

        @NotBlank(message = "내용을 입력해주세요.")
        private String content;        // 질문 내용

        private Boolean isSecret = false;  // 비밀글 여부 (기본값 false)
    }

    // 질문 수정 요청 DTO (일반회원)
    // 수정할 항목만 보내면 되므로 선택 입력
    @Getter @Setter
    public static class Update {
        // BoardRequestDto.Update.title과 동일한 이유로 "비어있지 않음"과 "길이 제한"을
        // 하나의 정규식으로 합침(교차검증에서 발견 — @Pattern과 @Size를 따로 두면 둘 다
        // 동시에 실패하는 입력에서 GlobalExceptionHandler가 한쪽 메시지를 덮어씀). "$" 대신
        // "\z"를 쓰는 이유도 BoardRequestDto.Update.title 참고(끝에 줄바꿈이 있으면 "$"로는
        // 길이 제한이 우회됨).
        @Pattern(regexp = "(?sU)(?=^.{0,200}\\z).*\\S.*", message = "제목을 1~200자로 입력해주세요.")
        private String title;          // 수정할 제목 (선택)

        @Pattern(regexp = "(?sU).*\\S.*", message = "내용을 입력해주세요.")
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