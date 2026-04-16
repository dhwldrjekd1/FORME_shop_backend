package com.forme.shop.order.dto;

import com.forme.shop.order.entity.OrderItem;
import com.forme.shop.order.entity.Orders;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// 클라이언트에게 주문 정보를 응답할 때 사용하는 DTO
@Getter
@Builder
public class OrderResponseDto {

    private Long id;
    private Long memberId;
    private String memberName;     // 주문자 이름
    private Integer totalPrice;    // 총 결제 금액
    private String status;         // 주문 상태
    private String receiverName;   // 수령인 이름
    private String receiverPhone;  // 수령인 전화번호
    private String address;        // 배송 주소
    private List<OrderItemResponseDto> orderItems;  // 주문 상세 목록
    private LocalDateTime createdAt;

    // Orders 엔티티를 OrderResponseDto 로 변환하는 정적 메서드
    public static OrderResponseDto from(Orders orders) {
        return OrderResponseDto.builder()
                .id(orders.getId())
                .memberId(orders.getMember().getId())
                .memberName(orders.getMember().getName())
                .totalPrice(orders.getTotalPrice())
                .status(orders.getStatus())
                .receiverName(orders.getReceiverName())
                .receiverPhone(orders.getReceiverPhone())
                .address(orders.getAddress())
                // 주문 상세 목록도 DTO 로 변환
                .orderItems(orders.getOrderItems().stream()
                        .map(OrderItemResponseDto::from)
                        .collect(Collectors.toList()))
                .createdAt(orders.getCreatedAt())
                .build();
    }

    // 주문 상세 응답 DTO (OrderResponseDto 안에 포함)
    @Getter
    @Builder
    public static class OrderItemResponseDto {
        private Long id;
        private Long productId;
        private String productName;
        private String productImageUrl;
        private String size;
        private Integer quantity;
        private Integer price;
        private Integer totalPrice;

        // OrderItem 엔티티를 OrderItemResponseDto 로 변환하는 정적 메서드
        public static OrderItemResponseDto from(OrderItem item) {
            return OrderItemResponseDto.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .productImageUrl(item.getProduct().getImageUrl())
                    .size(item.getSize())
                    .quantity(item.getQuantity())
                    .price(item.getUnitPrice())
                    // 총 금액 = 단가 * 수량 (Integer 곱셈)
                    .totalPrice(item.getUnitPrice() * item.getQuantity())
                    .build();
        }
    }
}