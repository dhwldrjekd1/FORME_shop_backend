package com.forme.shop.delivery.service;

import com.forme.shop.delivery.dto.DeliveryRequestDto;
import com.forme.shop.delivery.dto.DeliveryResponseDto;
import com.forme.shop.delivery.entity.Delivery;
import com.forme.shop.delivery.repository.DeliveryRepository;
import com.forme.shop.order.entity.Orders;
import com.forme.shop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    // 특정 주문의 배송 정보 조회
    public DeliveryResponseDto getDelivery(Long orderId) {
        Delivery delivery = deliveryRepository.findByOrdersId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("배송 정보가 없습니다."));
        return DeliveryResponseDto.from(delivery);
    }

    // 배송 상태별 목록 조회 (관리자)
    // ex: status = "IN_TRANSIT" → 배송중인 것만 조회
    public List<DeliveryResponseDto> getDeliveriesByStatus(String status) {
        return deliveryRepository.findByStatus(status)
                .stream()
                .map(DeliveryResponseDto::from)
                .collect(Collectors.toList());
    }

    // 배송 정보 등록 (관리자)
    // 주문 생성 후 배송 정보를 별도로 등록할 때 사용
    @Transactional
    public DeliveryResponseDto createDelivery(Long orderId, DeliveryRequestDto.Create dto) {
        // 주문 존재 여부 확인
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 이미 배송 정보가 있는지 확인
        if (deliveryRepository.findByOrdersId(orderId).isPresent()) {
            throw new IllegalArgumentException("이미 배송 정보가 등록되어 있습니다.");
        }

        Delivery delivery = Delivery.builder()
                .orders(orders)
                .carrier(dto.getCarrier())
                .trackingNumber(dto.getTrackingNumber())
                .build();

        return DeliveryResponseDto.from(deliveryRepository.save(delivery));
    }

    // 배송 정보 수정 (관리자)
    // 운송장 번호 입력, 배송 상태 변경 등
    @Transactional
    public DeliveryResponseDto updateDelivery(Long deliveryId, DeliveryRequestDto.Update dto) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배송 정보입니다."));

        // null 체크 후 값이 있을 때만 수정
        if (dto.getCarrier()        != null) delivery.setCarrier(dto.getCarrier());
        if (dto.getTrackingNumber() != null) delivery.setTrackingNumber(dto.getTrackingNumber());

        // 배송 상태 변경 시 시간 자동 기록
        if (dto.getStatus() != null) {
            delivery.setStatus(dto.getStatus());

            // IN_TRANSIT 으로 변경 시 발송 시간 자동 기록
            if (dto.getStatus().equals("IN_TRANSIT") && delivery.getShippedAt() == null) {
                delivery.setShippedAt(LocalDateTime.now());
            }
            // DELIVERED 로 변경 시 배송 완료 시간 자동 기록
            if (dto.getStatus().equals("DELIVERED") && delivery.getDeliveredAt() == null) {
                delivery.setDeliveredAt(LocalDateTime.now());
            }
        }

        return DeliveryResponseDto.from(delivery);
    }
}