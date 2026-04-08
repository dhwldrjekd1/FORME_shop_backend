package com.bluebottle.shop.review.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

// DTO(Data Transfer Object): 클라이언트 ↔ 서버 간 데이터 전달 전용 객체
public class ReviewRequestDto {

    // 리뷰 작성 요청 DTO
    @Getter @Setter
    public static class Create {

        @NotNull(message = "주문을 선택해주세요.")
        private Long orderId;      // 어떤 주문에 대한 리뷰인지

        @NotNull(message = "상품을 선택해주세요.")
        private Long productId;    // 어떤 상품에 대한 리뷰인지

        @NotNull(message = "별점을 입력해주세요.")
        @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
        private Integer rating;    // 별점 (1~5)

        @NotBlank(message = "리뷰 내용을 입력해주세요.")
        private String content;    // 리뷰 내용
    }

    // 리뷰 수정 요청 DTO
    @Getter @Setter
    public static class Update {

        @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
        private Integer rating;    // 수정할 별점 (선택)

        private String content;    // 수정할 내용 (선택)
    }
}