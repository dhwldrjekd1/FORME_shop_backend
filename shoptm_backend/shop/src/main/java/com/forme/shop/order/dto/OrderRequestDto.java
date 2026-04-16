package com.forme.shop.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class OrderRequestDto {

    // 주문 요청 DTO
    @Getter @Setter
    public static class Create {

        @NotNull(message = "주문 상품을 선택해주세요.")
        private List<OrderItemDto> items;  // 주문할 상품 목록

        @NotBlank(message = "수령인 이름을 입력해주세요.")
        private String receiverName;       // 수령인 이름

        @NotBlank(message = "수령인 전화번호를 입력해주세요.")
        private String receiverPhone;      // 수령인 전화번호

        @NotBlank(message = "배송 주소를 입력해주세요.")
        private String address;            // 배송 주소
    }

    // 주문 상품 항목 DTO
    @Getter @Setter
    public static class OrderItemDto {

        @NotNull(message = "상품을 선택해주세요.")
        private Long productId;            // 주문할 상품 ID

        @NotNull(message = "수량을 입력해주세요.")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        private Integer quantity;          // 주문 수량

        private String size;               // 주문 사이즈
    }

    // 주문 상태 변경 DTO (관리자)
    @Getter @Setter
    public static class UpdateStatus {

        @NotBlank(message = "상태를 입력해주세요.")
        private String status;             // 변경할 상태 (SHIPPING/DELIVERED/CANCELLED)
    }
}