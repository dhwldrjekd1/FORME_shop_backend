package com.forme.shop.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class OrderRequestDto {

    // 주문 요청 DTO
    @Getter @Setter
    public static class Create {

        // @Valid가 없으면 컨테이너(List) 안 OrderItemDto 각 원소의 @NotNull/@Min 등이
        // 전혀 검증되지 않는다 — Bean Validation은 필드 자체에 @Valid가 있어야 원소까지 내려간다.
        // @Size(max): 이 목록 길이에 상한이 없으면, 로그인한 사용자가 API를 직접 호출해 품목을
        // 수천 개 실어 보낼 수 있었다 — OrderService.createOrder()가 품목마다 적어도 하나 이상의
        // DB 쿼리(상품 조회 + 재고 원자적 UPDATE, 사이즈 관리 상품이면 그만큼 더)를 같은
        // 트랜잭션 안에서 순차로 실행하므로, 그런 요청 하나가 트랜잭션을 오래 붙잡으며 다른
        // 주문의 재고 UPDATE까지 줄줄이 대기시킬 수 있다(다른 방향에서 버그를 찾다가 발견).
        // 실제 장바구니 화면은 이 상한에 걸릴 만큼 품목을 담을 일이 없어, 정상 사용에는 영향이 없다.
        @NotEmpty(message = "주문 상품을 선택해주세요.")
        @Size(max = 100, message = "한 번에 주문할 수 있는 상품 종류는 100개 이하여야 합니다.")
        @Valid
        private List<OrderItemDto> items;  // 주문할 상품 목록

        // Orders.receiverName 컬럼 길이(50)와 맞춤 — 없으면 DB가 거부하는 DataIntegrityViolationException이
        // 그대로 500으로 노출됨(교차검증에서 발견 — 이런 종류의 길이 제한 누락이 이 DTO
        // 전체에 공통된 문제). @Size는 값이 null이면 통과시키므로 @NotBlank와 함께 둬야 함.
        @NotBlank(message = "수령인 이름을 입력해주세요.")
        @Size(max = 50, message = "수령인 이름은 50자 이하여야 합니다.")
        private String receiverName;       // 수령인 이름

        // Orders.receiverPhone 컬럼 길이(20)와 맞춤
        @NotBlank(message = "수령인 전화번호를 입력해주세요.")
        @Size(max = 20, message = "수령인 전화번호는 20자 이하여야 합니다.")
        private String receiverPhone;      // 수령인 전화번호

        // Orders.address 컬럼은 길이 지정이 없어 Hibernate 기본값(255)을 따름
        @NotBlank(message = "배송 주소를 입력해주세요.")
        @Size(max = 255, message = "배송 주소는 255자 이하여야 합니다.")
        private String address;            // 배송 주소

        // 참고용 — 실제 PAID 판정에는 쓰이지 않는다. paymentKey로 서버가 확인한 결제가 있을
        // 때만 그 결제의 실제 승인 금액(Payment.amount)을 기준으로 PAID 처리하며, 이 필드값은
        // 그때도 무시된다. paymentKey 없이 이 값만 보내는 것으로는 주문이 PAID가 될 수 없고
        // 항상 PENDING(결제 없는 데모 주문)으로 생성된다 (OrderService.createOrder)
        private Integer paidAmount;

        // 토스 결제 승인(confirm) 시 발급된 paymentKey — 있으면 주문 생성 실패 시
        // 이 결제를 자동으로 취소(환불)하는 데 사용됨 (OrderService.createOrder)
        private String paymentKey;
    }

    // 주문 상품 항목 DTO
    @Getter @Setter
    public static class OrderItemDto {

        @NotNull(message = "상품을 선택해주세요.")
        private Long productId;            // 주문할 상품 ID

        @NotNull(message = "수량을 입력해주세요.")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        @Max(value = 999, message = "수량은 999개 이하여야 합니다.")
        private Integer quantity;          // 주문 수량

        // OrderItem.size 컬럼 길이(20)와 맞춤
        @Size(max = 20, message = "사이즈는 20자 이하여야 합니다.")
        private String size;               // 주문 사이즈
    }

    // 주문 상태 변경 DTO (관리자)
    @Getter @Setter
    public static class UpdateStatus {

        @NotBlank(message = "상태를 입력해주세요.")
        private String status;             // 변경할 상태 (SHIPPING/DELIVERED/CANCELLED)
    }
}