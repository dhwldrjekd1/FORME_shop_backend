package com.bluebottle.shop.delivery.dto;

import com.bluebottle.shop.delivery.entity.Delivery;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

// 클라이언트에게 배송 정보를 응답할 때 사용하는 DTO
@Getter
@Builder
public class DeliveryResponseDto {

    private Long id;
    private Long orderId;          // 주문 ID
    private String carrier;        // 택배사
    private String trackingNumber; // 운송장 번호
    private String status;         // 배송 상태
    private LocalDateTime shippedAt;    // 발송 시간
    private LocalDateTime deliveredAt;  // 배송 완료 시간
    private LocalDateTime createdAt;

    // Delivery 엔티티를 DeliveryResponseDto 로 변환하는 정적 메서드
    public static DeliveryResponseDto from(Delivery delivery) {
        return DeliveryResponseDto.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrders().getId())
                .carrier(delivery.getCarrier())
                .trackingNumber(delivery.getTrackingNumber())
                .status(delivery.getStatus())
                .shippedAt(delivery.getShippedAt())
                .deliveredAt(delivery.getDeliveredAt())
                .createdAt(delivery.getCreatedAt())
                .build();
    }
}