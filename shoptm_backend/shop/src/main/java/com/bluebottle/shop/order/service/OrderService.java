package com.bluebottle.shop.order.service;

import com.bluebottle.shop.member.entity.Member;
import com.bluebottle.shop.member.repository.MemberRepository;
import com.bluebottle.shop.order.dto.OrderRequestDto;
import com.bluebottle.shop.order.dto.OrderResponseDto;
import com.bluebottle.shop.order.entity.OrderItem;
import com.bluebottle.shop.order.entity.Orders;
import com.bluebottle.shop.order.repository.OrderRepository;
import com.bluebottle.shop.product.entity.Product;
import com.bluebottle.shop.product.repository.ProductRepository;
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

            // 주문 상세 생성
            OrderItem orderItem = OrderItem.builder()
                    .orders(orders)
                    .product(product)
                    .quantity(itemDto.getQuantity())
                    .unitPrice(product.getPrice())  //  price → unitPrice 로 변경
                    .build();

            orders.getOrderItems().add(orderItem);

            // 총액 계산 (단가 * 수량)
            totalPrice += product.getPrice() * itemDto.getQuantity();  //  Integer 덧셈
        }

        orders.setTotalPrice(totalPrice);  // 총액 설정
        return OrderResponseDto.from(orderRepository.save(orders));
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