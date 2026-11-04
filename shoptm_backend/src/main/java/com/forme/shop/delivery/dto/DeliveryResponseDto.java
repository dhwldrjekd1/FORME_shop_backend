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

    // 이 배송이 속한 주문의 "현재" 상태(Orders.status) — DeliveryService.updateDelivery()가
    // 배송 상태 변경에 맞춰 주문 상태도 같이 동기화하는데(OrderService.syncStatusFromDelivery
    // 참고), 그 결과를 관리자 화면(AdminOrders.vue)이 바로 반영하려면 이 응답에 최신 값이
    // 실려 있어야 한다. 처음엔 프론트가 "상태가 바뀌었는지"를 자기가 들고 있는 값과 비교해
    // 바뀌었을 때만 주문을 다시 조회하려 했는데, 그 비교에 쓰려던 값이 <select v-model>로
    // 이미 새 값으로 바뀌어 있는 바로 그 객체였어서 비교 자체가 항상 거짓이 되는 문제가
    // 있었다(교차검증에서 발견) — 애초에 서버가 최신 값을 그대로 응답에 실어주면 프론트가
    // 그런 비교를 할 필요도, 추가로 재조회할 필요도 없어진다.
    private String orderStatus;

    // Delivery 엔티티를 DeliveryResponseDto 로 변환하는 정적 메서드. 주문 상태 동기화가 없는
    // 호출부(getDelivery/createDelivery)는 delivery.getOrders()에서 바로 읽어도 항상 최신값이다.
    public static DeliveryResponseDto from(Delivery delivery) {
        return from(delivery, delivery.getOrders().getStatus());
    }

    // 주문 상태 동기화가 있는 호출부(DeliveryService.updateDelivery)는 동기화 뒤 다시 읽은
    // 최신 주문 상태를 명시적으로 넘긴다 — 동기화에 쓰는 원자적 UPDATE가 영속성 컨텍스트를
    // 비워서, 그 뒤에 delivery.getOrders().getStatus()를 다시 읽으면 갱신 전의 캐시된 값이
    // 나올 수 있다.
    public static DeliveryResponseDto from(Delivery delivery, String orderStatus) {
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
                .orderStatus(orderStatus)
                .build();
    }
}