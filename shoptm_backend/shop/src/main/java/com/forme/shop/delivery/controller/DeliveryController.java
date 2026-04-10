package com.forme.shop.delivery.controller;

import com.forme.shop.delivery.dto.DeliveryRequestDto;
import com.forme.shop.delivery.dto.DeliveryResponseDto;
import com.forme.shop.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    // 특정 주문의 배송 정보 조회 (일반회원)
    // GET /api/orders/{orderId}/delivery
    @GetMapping("/orders/{orderId}/delivery")
    public ResponseEntity<DeliveryResponseDto> getDelivery(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryService.getDelivery(orderId));
    }

    // 배송 상태별 목록 조회 (관리자)
    // GET /api/admin/deliveries?status=IN_TRANSIT
    @GetMapping("/admin/deliveries")
    public ResponseEntity<List<DeliveryResponseDto>> getDeliveriesByStatus(
            @RequestParam(required = false, defaultValue = "READY") String status) {
        // required = false: status 파라미터 없으면 기본값 READY 사용
        return ResponseEntity.ok(deliveryService.getDeliveriesByStatus(status));
    }

    // 배송 정보 등록 (관리자)
    // POST /api/admin/orders/{orderId}/delivery
    @PostMapping("/admin/orders/{orderId}/delivery")
    public ResponseEntity<DeliveryResponseDto> createDelivery(
            @PathVariable Long orderId,
            @Valid @RequestBody DeliveryRequestDto.Create dto) {
        return ResponseEntity.ok(deliveryService.createDelivery(orderId, dto));
    }

    // 배송 정보 수정 (관리자)
    // PUT /api/admin/deliveries/{deliveryId}
    @PutMapping("/admin/deliveries/{deliveryId}")
    public ResponseEntity<DeliveryResponseDto> updateDelivery(
            @PathVariable Long deliveryId,
            @Valid @RequestBody DeliveryRequestDto.Update dto) {
        return ResponseEntity.ok(deliveryService.updateDelivery(deliveryId, dto));
    }
}