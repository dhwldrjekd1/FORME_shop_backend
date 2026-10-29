package com.forme.shop.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class BoardRequestDto {

    // 게시글 작성 요청 DTO
    @Getter @Setter
    public static class Create {

        // Board.title 컬럼 길이(200)와 맞춤 — 없으면 DB가 거부하는
        // DataIntegrityViolationException이 그대로 500으로 노출됨(교차검증에서 발견).
        // content는 TEXT 컬럼이라 DB 쪽 길이 제한이 없어 여기서는 다루지 않음.
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        private String title;          // 게시글 제목

        @NotBlank(message = "내용을 입력해주세요.")
        private String content;        // 게시글 내용
    }

    // 게시글 수정 요청 DTO
    // 수정할 항목만 보내면 되므로 선택 입력
    @Getter @Setter
    public static class Update {
        // @Pattern과 @Size를 따로 두면 둘 다 동시에 실패하는 입력(예: 공백만 201자)에서
        // GlobalExceptionHandler가 필드별 에러를 HashMap에 담다가 한쪽 메시지를 덮어써버리는
        // 문제가 있어(교차검증에서 발견), "비어있지 않음"과 "길이 제한"을 하나의 정규식으로
        // 합쳐 이 필드에 실패 가능한 제약을 하나만 둔다 — lookahead로 길이(0~200자)를 먼저
        // 확인하고, 그 뒤 기존과 동일하게 공백 아닌 문자가 하나 이상 있는지 확인. 길이를
        // "$"로 재면 Java 정규식에서 "$"가 끝의 줄바꿈 문자 바로 앞에도 매칭돼(교차검증에서
        // 발견 — 실제로 "a"*200 + "\n"(201자)가 "$"로는 통과해버림을 재현), 입력 끝을
        // 엄격히 보는 "\z"를 대신 쓴다.
        @Pattern(regexp = "(?sU)(?=^.{0,200}\\z).*\\S.*", message = "제목을 1~200자로 입력해주세요.")
        private String title;          // 수정할 제목 (선택)

        @Pattern(regexp = "(?sU).*\\S.*", message = "내용을 입력해주세요.")
        private String content;        // 수정할 내용 (선택)
    }
}