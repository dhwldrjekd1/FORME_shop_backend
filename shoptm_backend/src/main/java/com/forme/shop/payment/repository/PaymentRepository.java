package com.forme.shop.payment.repository;

import com.forme.shop.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentKey(String paymentKey);

    // 주문 취소 시 그 주문에 연결된 결제를 찾아 환불하는 데 사용 (markLinked로 연결된 것만 찾아짐 —
    // 결제 없이 생성된 데모 주문은 연결된 결제가 없으므로 여기서 조회되지 않는 게 정상).
    // order_id에는 DB 유니크 제약이 없어(FK만 있음) 한 주문에 결제 기록이 두 개 이상 붙는 이상
    // 상황이 이론적으로 가능하다 — Optional 대신 List로 받아 그런 경우에도 예외를 던지거나
    // 하나만 골라 나머지를 놓치지 않고 전부 환불 대상으로 처리할 수 있게 한다.
    List<Payment> findByOrders_Id(Long orderId);

    // 주문 취소가 커밋되는 바로 그 트랜잭션 안에서(=재고 복구 등과 원자적으로 함께) 연결된
    // 결제가 있으면 "환불 대기중"으로 표시해둔다. 실제 토스 취소 API 호출은 이 커밋이 끝난
    // 뒤(afterCommit)에 이어서 시도하지만, 혹시 그 사이 프로세스가 죽는 등으로 그 시도 자체가
    // 아예 못 일어나더라도 이 상태만은 이미 커밋돼 DB에 남아있으므로 "환불해야 하는데 아무
    // 기록도 없이 조용히 유실됨"은 막을 수 있다 — 나중에 이 상태로 남아있는 결제를 찾아
    // 수동으로 처리할 수 있음. 이미 REFUNDED/REFUND_FAILED인 것은 건드리지 않는다.
    // updatedAt도 함께 갱신 — @UpdateTimestamp는 벌크 JPQL UPDATE 경로를 안 타므로(엔티티
    // setter를 거치는 경로에서만 자동 갱신됨) OrderRepository의 취소 쿼리들과 동일하게 직접
    // 세팅해준다. 이게 없으면 REFUND_PENDING으로 멈춰버린 결제를 나중에 찾았을 때, 그게 방금
    // 발생한 건지 며칠 전 것인지(markLinked 시점 값 그대로) 구분할 수 없다.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Payment p SET p.status = 'REFUND_PENDING', p.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE p.orders.id = :orderId AND p.status NOT IN ('REFUNDED', 'REFUND_FAILED')")
    int markRefundPendingForOrder(@Param("orderId") Long orderId);

    // 이 결제를 "지금 이 요청이" 주문 생성에 쓰겠다고 선점(claim)한다.
    // CONFIRMED 상태일 때만 PROCESSING으로 바꾸는 조건부 원자적 UPDATE라서, 같은 paymentKey로
    // 동시에 여러 요청이 들어와도(더블클릭, 네트워크 재시도) 단 하나만 1을 반환하며 성공한다.
    // (재고 차감의 decreaseStockIfAvailable과 동일한 패턴)
    // 반환값(영향받은 행 수)이 0이면 이미 다른 요청이 선점했거나, 이미 주문으로 연결됐거나, 취소된 결제.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Payment p SET p.status = 'PROCESSING' WHERE p.paymentKey = :paymentKey AND p.status = 'CONFIRMED'")
    int claimIfConfirmed(@Param("paymentKey") String paymentKey);
}
