package com.forme.shop.delivery.dto;

import com.forme.shop.delivery.entity.Delivery;
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
    private LocalDateTime updatedAt;    // 수정 시간 — updateDelivery 요청의 expectedUpdatedAt에
                                         // 그대로 실어 보내 동시수정 충돌을 감지하는 데 쓰임

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
                .updatedAt(delivery.getUpdatedAt())
                .build();
    }
}