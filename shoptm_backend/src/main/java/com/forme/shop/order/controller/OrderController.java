package com.forme.shop.order.controller;

import com.forme.shop.order.dto.OrderRequestDto;
import com.forme.shop.order.dto.OrderResponseDto;
import com.forme.shop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController               // @Controller + @ResponseBody: JSON 형태로 응답 반환
@RequestMapping("/api")       // 이 컨트롤러의 모든 URL 앞에 /api 붙음
@RequiredArgsConstructor      // Lombok: final 필드 생성자 주입 자동 처리
public class OrderController {

    private final OrderService orderService;

    // 주문 생성 (일반회원)
    // POST /api/members/{memberId}/orders
    @PostMapping("/members/{memberId}/orders")
    public ResponseEntity<OrderResponseDto> createOrder(
            @PathVariable Long memberId,
            @Valid @RequestBody OrderRequestDto.Create dto) {
        return ResponseEntity.ok(orderService.createOrder(memberId, dto));
    }

    // 내 주문 목록 조회 (일반회원)
    // GET /api/members/{memberId}/orders
    @GetMapping("/members/{memberId}/orders")
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(
            @PathVariable Long memberId) {
        return ResponseEntity.ok(orderService.getMyOrders(memberId));
    }

    // 주문 단건 조회
    // GET /api/orders/{orderId}
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrder(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    // 주문 취소 (일반회원)
    // PATCH /api/orders/{orderId}/cancel
    @PatchMapping("/orders/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    // 관리자 - 전체 주문 목록. 회원목록과 같은 이유로 페이지네이션 적용(교차검증에서 발견).
    // 정렬은 createdAt DESC로 고정(OrderService.getAllOrders 참고 — fetch join과 Pageable의
    // sort를 함께 쓸 수 없는 구조적 한계) 이라 sort 파라미터는 받지 않는다.
    // GET /api/admin/orders?page=0&size=20
    @GetMapping("/admin/orders")
    public ResponseEntity<Page<OrderResponseDto>> getAllOrders(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    // 관리자 - 주문 상태 변경 (배송처리 등)
    // PATCH /api/admin/orders/{orderId}/status
    @PatchMapping("/admin/orders/{orderId}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderRequestDto.UpdateStatus dto) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, dto));
    }
}