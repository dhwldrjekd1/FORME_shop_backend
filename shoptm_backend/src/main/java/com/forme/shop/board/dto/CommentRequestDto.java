package com.forme.shop.board.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// DTO(Data Transfer Object): 클라이언트 ↔ 서버 간 데이터 전달 전용 객체
public class CommentRequestDto {

    // 댓글 작성 요청 DTO
    @Getter @Setter
    public static class Create {

        @NotBlank(message = "댓글 내용을 입력해주세요.")
        private String content;        // 댓글 내용
    }

    // 댓글 수정 요청 DTO
    @Getter @Setter
    public static class Update {

        @NotBlank(message = "댓글 내용을 입력해주세요.")
        private String content;        // 수정할 댓글 내용
    }
}