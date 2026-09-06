package com.forme.shop.order.scheduler;

import com.forme.shop.order.dto.OrderRequestDto;
import com.forme.shop.order.repository.OrderRepository;
import com.forme.shop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// 결제 없이 생성된 데모 주문(PENDING)은 생성 시점에 이미 실제 재고를 차감해가지만, 결제를
// 거치지 않으므로 그 자체로는 정리될 계기가 없다 — 누군가 수동으로 취소하지 않는 한, 실제로
// 팔리지도 않은 재고가 영구히 묶여있게 된다(OrderService.updateMemberGrade 주석과 같은
// 맥락 — PENDING 주문은 등급 산정에서도 제외되도록 별도로 고쳐져 있음). 일정 시간이 지나도록
// 그대로 PENDING인 주문은 여기서 주기적으로 자동 취소해 재고를 풀어준다.
//
// updateOrderStatus를 그대로 재사용하는 이유: 취소 시 원자적 상태 전이/재고 복구/환불 필요
// 여부 판단(paidAt 기준)이 이미 거기 다 구현돼 있고, 여기서 같은 로직을 다시 만들면 두 곳이
// 서로 달라질 위험만 생긴다. PENDING 주문은 paidAt이 없으므로 이 경로를 타도 환불 시도는
// 자동으로 스킵된다(PaymentService.refundForCancellation 참고).
@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryScheduler.class);

    // 정상 결제(토스) 플로우는 이미 결제가 확정된 뒤에만 주문을 생성한다(PaymentView.vue —
    // paymentKey를 돌려받은 뒤에만 createOrder 호출). 그래서 이 시스템에서 PENDING으로 남는
    // 주문은 전부 결제 없는 데모 주문뿐이고, "결제 진행 중이라 아직 PENDING"인 정상 케이스는
    // 없다 — 짧게 잡아도 진행 중인 결제를 잘못 끊을 위험은 없다는 뜻. 그래도 관리자가 데모
    // 주문을 수동으로 확인/처리(무통장입금 확인 후 발송과 비슷한 방식)할 시간은 남겨두기
    // 위해 24시간으로 둔다.
    private static final long EXPIRY_HOURS = 24;

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    // 매 시간 한 번씩 확인 (서버 기동 1분 뒤부터).
    @Scheduled(fixedDelay = 60 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void expireStalePendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(EXPIRY_HOURS);
        List<Long> staleOrderIds = orderRepository.findIdsByStatusAndCreatedAtBefore("PENDING", cutoff);
        for (Long orderId : staleOrderIds) {
            try {
                OrderRequestDto.UpdateStatus dto = new OrderRequestDto.UpdateStatus();
                dto.setStatus("CANCELLED");
                orderService.updateOrderStatus(orderId, dto);
                log.info("결제 없이 {}시간 넘게 방치된 데모 주문을 자동 취소함 (orderId={})", EXPIRY_HOURS, orderId);
            } catch (Exception e) {
                // 한 건이 실패해도(그 사이 다른 경로로 이미 처리됐거나 등) 나머지 만료 대상은
                // 계속 처리한다.
                log.error("만료된 데모 주문 자동 취소 실패 (orderId={})", orderId, e);
            }
        }
    }
}
