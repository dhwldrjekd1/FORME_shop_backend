package com.forme.shop.order.service;

import com.forme.shop.member.entity.Member;
import com.forme.shop.member.repository.MemberRepository;
import com.forme.shop.order.dto.OrderRequestDto;
import com.forme.shop.order.dto.OrderResponseDto;
import com.forme.shop.order.entity.OrderItem;
import com.forme.shop.order.entity.Orders;
import com.forme.shop.order.repository.OrderRepository;
import com.forme.shop.product.entity.Product;
import com.forme.shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    // 주문 생성 (일반회원)
    @Transactional
    public OrderResponseDto createOrder(Long memberId, OrderRequestDto.Create dto) {

        // 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 주문 엔티티 생성 (총액은 아래에서 계산 후 설정)
        Orders orders = Orders.builder()
                .member(member)
                .receiverName(dto.getReceiverName())
                .receiverPhone(dto.getReceiverPhone())
                .address(dto.getAddress())
                .totalPrice(0)  //  BigDecimal.ZERO → 0 으로 변경
                .build();

        // 주문 상품 목록 처리
        int totalPrice = 0;  //  BigDecimal → int 로 변경

        for (OrderRequestDto.OrderItemDto itemDto : dto.getItems()) {
            // 상품 존재 여부 확인
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

            // 재고 확인
            if (product.getStock() < itemDto.getQuantity()) {
                throw new IllegalArgumentException(product.getName() + "의 재고가 부족합니다.");
            }

            // 재고 차감
            product.setStock(product.getStock() - itemDto.getQuantity());

            // 세일 할인 적용된 단가 계산
            int unitPrice = product.getPrice();
            if (product.getDiscountRate() != null && product.getDiscountRate() > 0) {
                unitPrice = (int) Math.round(product.getPrice() * (1 - product.getDiscountRate() / 100.0));
            }

            // 등급 할인 적용
            int gradeDiscount = getGradeDiscount(member.getGrade());
            if (gradeDiscount > 0) {
                unitPrice = (int) Math.round(unitPrice * (1 - gradeDiscount / 100.0));
            }

            OrderItem orderItem = OrderItem.builder()
                    .orders(orders)
                    .product(product)
                    .quantity(itemDto.getQuantity())
                    .unitPrice(unitPrice)
                    .size(itemDto.getSize())
                    .build();

            orders.getOrderItems().add(orderItem);

            totalPrice += unitPrice * itemDto.getQuantity();
        }

        orders.setTotalPrice(totalPrice);
        OrderResponseDto result = OrderResponseDto.from(orderRepository.save(orders));

        // 누적 구매 금액 기반 등급 자동 업그레이드
        updateMemberGrade(member);

        return result;
    }

    // 등급별 할인율
    private int getGradeDiscount(String grade) {
        if (grade == null) return 0;
        return switch (grade.toUpperCase()) {
            case "SILVER" -> 5;
            case "GOLD" -> 8;
            case "VIP" -> 12;
            default -> 0;
        };
    }

    // 누적 구매 금액 계산 → 등급 자동 변경
    private void updateMemberGrade(Member member) {
        // 취소 제외 전체 주문 금액 합산
        int totalSpent = orderRepository.findByMemberIdOrderByCreatedAtDesc(member.getId())
                .stream()
                .filter(o -> !"CANCELLED".equals(o.getStatus()))
                .mapToInt(Orders::getTotalPrice)
                .sum();

        String newGrade;
        if (totalSpent >= 1500000) {
            newGrade = "VIP";        // 150만원 이상
        } else if (totalSpent >= 1000000) {
            newGrade = "GOLD";       // 100만원 이상
        } else if (totalSpent >= 500000) {
            newGrade = "SILVER";     // 50만원 이상
        } else {
            newGrade = "BRONZE";     // 50만원 미만
        }

        if (!newGrade.equals(member.getGrade())) {
            member.setGrade(newGrade);
            // @Transactional이므로 자동 저장
        }
    }

    // 내 주문 목록 조회 (일반회원)
    public List<OrderResponseDto> getMyOrders(Long memberId) {
        return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(OrderResponseDto::from)
                .collect(Collectors.toList());
    }

    // 주문 단건 조회
    public OrderResponseDto getOrder(Long orderId) {
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        return OrderResponseDto.from(orders);
    }

    // 주문 취소 (일반회원)
    // PAID 상태일 때만 취소 가능
    @Transactional
    public void cancelOrder(Long orderId) {
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // PAID 상태가 아니면 취소 불가
        if (!orders.getStatus().equals("PAID")) {
            throw new IllegalArgumentException("취소할 수 없는 주문입니다.");
        }

        // 재고 복구
        for (OrderItem item : orders.getOrderItems()) {
            item.getProduct().setStock(
                    item.getProduct().getStock() + item.getQuantity());
        }

        orders.setStatus("CANCELLED");  // 주문 상태 취소로 변경
    }

    // 관리자 - 전체 주문 목록 조회
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(OrderResponseDto::from)
                .collect(Collectors.toList());
    }

    // 관리자 - 주문 상태 변경
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, OrderRequestDto.UpdateStatus dto) {
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        orders.setStatus(dto.getStatus());  // 상태 변경 (더티 체킹으로 자동 저장)
        return OrderResponseDto.from(orders);
    }
}