package com.forme.shop.order.service;

import com.forme.shop.common.security.SecurityUtil;
import com.forme.shop.member.entity.Member;
import com.forme.shop.member.repository.MemberRepository;
import com.forme.shop.order.dto.OrderRequestDto;
import com.forme.shop.order.dto.OrderResponseDto;
import com.forme.shop.order.entity.OrderItem;
import com.forme.shop.order.entity.Orders;
import com.forme.shop.order.repository.OrderRepository;
import com.forme.shop.payment.entity.Payment;
import com.forme.shop.payment.service.PaymentService;
import com.forme.shop.product.entity.Product;
import com.forme.shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;

    // 주문 생성 (일반회원)
    @Transactional
    public OrderResponseDto createOrder(Long memberId, OrderRequestDto.Create dto) {

        // 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 본인(또는 관리자)만 자신의 이름으로 주문 생성 가능
        SecurityUtil.checkOwnerOrAdmin(member.getEmail());

        // 토스 결제를 거쳐 들어온 주문인지 (승인 기록이 실제로 존재하는 경우만) — 있으면
        // 아래에서 주문 생성이 실패했을 때 이미 승인된 결제를 자동으로 취소(환불)한다.
        String paymentKey = dto.getPaymentKey();
        Optional<Payment> confirmedPayment = paymentKey != null
                ? paymentService.findConfirmed(paymentKey) : Optional.empty();
        boolean paymentConfirmed = confirmedPayment.isPresent();
        // 결제 금액 검증은 클라이언트가 요청 본문에 실어 보낸 paidAmount가 아니라,
        // 결제 승인 시점에 서버가 이미 저장해 둔 진짜 승인 금액(Payment.amount)을 기준으로 한다.
        // (그렇지 않으면 실제로는 소액만 결제한 뒤 paidAmount만 크게 조작해 보내는 위변조가 가능함)
        Integer paidAmount = paymentConfirmed ? confirmedPayment.get().getAmount() : dto.getPaidAmount();

        Orders savedOrders;
        try {
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

                // 재고 확인 + 차감을 원자적 UPDATE 한 번으로 처리 (동시 주문으로 인한 오버셀 방지)
                int updated = productRepository.decreaseStockIfAvailable(product.getId(), itemDto.getQuantity());
                if (updated == 0) {
                    throw new IllegalArgumentException(product.getName() + "의 재고가 부족합니다.");
                }

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

            // 실제 결제 승인 금액(paidAmount)이 서버가 방금 계산한 진짜 주문 금액(totalPrice)과
            // 정확히 같은지 검증한다. (금액 위변조 방지 — 위에서 이미 클라이언트 값이 아니라
            // 서버에 저장된 Payment.amount로 대체됐으므로, 여기서는 그 값만 신뢰한다)
            if (paidAmount != null) {
                if (!paidAmount.equals(totalPrice)) {
                    throw new IllegalArgumentException(
                            "결제 금액과 주문 금액이 일치하지 않습니다. (결제: " + paidAmount + "원, 주문: " + totalPrice + "원)");
                }
                orders.setStatus("PAID");
                orders.setPaidAt(LocalDateTime.now());
            }

            savedOrders = orderRepository.save(orders);
        } catch (RuntimeException e) {
            // 이미 결제가 승인된 뒤였다면(재고 부족 등 어떤 이유로든) 주문을 만들지 못한 채
            // 카드만 결제된 상태로 남지 않도록 여기서 즉시 자동 환불한다.
            if (paymentConfirmed) {
                boolean refunded = paymentService.refundAndMarkFailed(paymentKey, e.getMessage());
                // 우리가 직접 던진, 사용자에게 보여줘도 안전한 메시지만 그대로 노출하고
                // 예기치 못한 내부 예외(NPE, DB 오류 등)의 원문 메시지는 클라이언트에 흘리지 않는다.
                if (!(e instanceof IllegalArgumentException)) {
                    log.error("주문 생성 중 예기치 못한 오류 (paymentKey={})", paymentKey, e);
                }
                String detail = (e instanceof IllegalArgumentException) ? e.getMessage() : "일시적인 오류가 발생했습니다.";
                throw new IllegalArgumentException(refunded
                        ? "주문 처리 중 오류가 발생해 결제가 자동으로 취소되었습니다. (" + detail + ")"
                        : "주문 처리 중 오류가 발생했고 결제 자동 취소에도 실패했습니다. 고객센터로 문의해주세요. (" + detail + ")");
            }
            throw e;
        }

        OrderResponseDto result = OrderResponseDto.from(savedOrders);

        // 결제 연결/등급 재계산은 부가 처리일 뿐이므로, 여기서 실패하더라도 이미 정상 저장된
        // 주문 자체를 롤백시키거나(전체 트랜잭션 실패) 환불을 잘못 트리거해서는 안 된다.
        if (paymentConfirmed) {
            try {
                paymentService.markLinked(paymentKey, savedOrders);
            } catch (Exception e) {
                log.error("결제-주문 연결 실패 (paymentKey={}, orderId={}) — 주문 자체는 정상 생성됨",
                        paymentKey, savedOrders.getId(), e);
            }
        }

        try {
            // 누적 구매 금액 기반 등급 자동 업그레이드
            updateMemberGrade(member);
        } catch (Exception e) {
            log.error("등급 자동 업그레이드 실패 (memberId={}, orderId={}) — 주문 자체는 정상 생성됨",
                    member.getId(), savedOrders.getId(), e);
        }

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
            // 이 시점의 member는 위 decreaseStockIfAvailable()의 clearAutomatically=true로
            // 영속성 컨텍스트가 비워지면서 이미 detach된 상태라, 더티 체킹으로는 저장되지 않는다.
            // (그래서 명시적으로 save 해야 함 - detached 엔티티도 id가 있으면 merge로 정상 반영됨)
            memberRepository.save(member);
        }
    }

    // 내 주문 목록 조회 (일반회원)
    public List<OrderResponseDto> getMyOrders(Long memberId) {
        // 본인(또는 관리자)의 주문 목록만 조회 가능
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        SecurityUtil.checkOwnerOrAdmin(member.getEmail());

        return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(OrderResponseDto::from)
                .collect(Collectors.toList());
    }

    // 주문 단건 조회
    public OrderResponseDto getOrder(Long orderId) {
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 본인(또는 관리자) 소유의 주문만 조회 가능
        SecurityUtil.checkOwnerOrAdmin(orders.getMember().getEmail());

        return OrderResponseDto.from(orders);
    }

    // 주문 취소 (일반회원)
    // PAID 상태일 때만 취소 가능
    @Transactional
    public void cancelOrder(Long orderId) {
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 본인(또는 관리자) 소유의 주문만 취소 가능
        SecurityUtil.checkOwnerOrAdmin(orders.getMember().getEmail());

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

    private static final java.util.Set<String> VALID_ORDER_STATUSES = java.util.Set.of(
            "PENDING", "PAID", "PREPARING", "SHIPPED", "DELIVERED", "CANCELLED");

    // 관리자 - 주문 상태 변경
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, OrderRequestDto.UpdateStatus dto) {
        if (!VALID_ORDER_STATUSES.contains(dto.getStatus())) {
            throw new IllegalArgumentException("올바르지 않은 주문 상태입니다.");
        }

        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 재고는 주문 생성 시점에 이미 차감돼 있으므로, 취소가 아니던 주문이 취소로
        // 바뀔 때는(회원 본인 취소 cancelOrder()와 동일하게) 재고를 복구해야 함.
        // 이미 취소된 주문을 다시 취소 처리해도 중복 복구되지 않도록 이전 상태를 확인.
        if ("CANCELLED".equals(dto.getStatus()) && !"CANCELLED".equals(orders.getStatus())) {
            for (OrderItem item : orders.getOrderItems()) {
                item.getProduct().setStock(
                        item.getProduct().getStock() + item.getQuantity());
            }
        }

        orders.setStatus(dto.getStatus());  // 상태 변경 (더티 체킹으로 자동 저장)
        return OrderResponseDto.from(orders);
    }
}