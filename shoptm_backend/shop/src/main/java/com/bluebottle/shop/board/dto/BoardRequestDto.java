package com.bluebottle.shop.board.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

public class BoardRequestDto {

    // 게시글 작성 요청 DTO
    @Getter @Setter
    public static class Create {

        @NotBlank(message = "제목을 입력해주세요.")
        private String title;          // 게시글 제목

        @NotBlank(message = "내용을 입력해주세요.")
        private String content;        // 게시글 내용
    }

    // 게시글 수정 요청 DTO
    // 수정할 항목만 보내면 되므로 선택 입력
    @Getter @Setter
    public static class Update {
        private String title;          // 수정할 제목 (선택)
        private String content;        // 수정할 내용 (선택)
    }
}