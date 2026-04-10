package com.forme.shop.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// DTO(Data Transfer Object): 클라이언트 ↔ 서버 간 데이터 전달 전용 객체
public class CartRequestDto {

    // 장바구니 담기 요청 DTO
    @Getter @Setter
    public static class Add {

        @NotNull(message = "상품을 선택해주세요.")
        private Long productId;    // 담을 상품 ID

        @NotNull(message = "수량을 입력해주세요.")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")  // 최소 1개
        private Integer quantity;  // 담을 수량
    }

    // 장바구니 수량 수정 요청 DTO
    @Getter @Setter
    public static class Update {

        @NotNull(message = "수량을 입력해주세요.")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")  // 최소 1개
        private Integer quantity;  // 변경할 수량
    }
}