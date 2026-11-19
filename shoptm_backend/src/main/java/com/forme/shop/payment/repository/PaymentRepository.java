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

    // 토스 승인(confirm) API 호출 자체가 타임아웃/5xx 등으로 응답을 못 받은 경우 — 토스 쪽에서는
    // 이미 카드 승인을 완료했을 가능성을 배제할 수 없는데, 그걸 그냥 "실패"로만 응답하고 끝내면
    // 이 결제가 시스템 어디에도 전혀 흔적이 안 남는다(PaymentService.recordConfirmAmbiguous 참고).
    // 같은 paymentKey로 이미 다른 상태의 행이 있으면(동시에 들어온 다른 요청이 먼저 확정
    // 성공/실패를 기록한 경우 등) 덮어쓰지 않고 그대로 둔다 — ON CONFLICT DO NOTHING으로,
    // 이 불확실한 기록이 이미 확정된 결과를 실수로 되돌리는 일이 없게 한다.
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO payments (payment_key, toss_order_id, amount, member_email, status, fail_reason, created_at, updated_at) " +
            "VALUES (:paymentKey, :tossOrderId, :amount, :memberEmail, 'CONFIRM_UNKNOWN', :failReason, now(), now()) " +
            "ON CONFLICT (payment_key) DO NOTHING",
            nativeQuery = true)
    int insertConfirmUnknownIfAbsent(@Param("paymentKey") String paymentKey, @Param("tossOrderId") String tossOrderId,
                                      @Param("amount") int amount, @Param("memberEmail") String memberEmail,
                                      @Param("failReason") String failReason);

    // recordConfirmed()가 쓰는 원자적 upsert. 같은 paymentKey로 이미 CONFIRM_UNKNOWN 행이
    // 있으면(응답 유실 후 재시도 confirm이 이번엔 성공한 경우) 그 불확실했던 기록을 확정
    // 상태로 갱신하고, 행 자체가 없으면 새로 만든다. 그 외 상태(이미 CONFIRMED로 정상
    // 처리된 경우 등)는 WHERE절에 안 걸려 DO UPDATE가 조용히 아무 일도 하지 않는다 —
    // Postgres의 조건부 ON CONFLICT DO UPDATE ... WHERE 동작.
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO payments (payment_key, toss_order_id, amount, member_email, status, created_at, updated_at) " +
            "VALUES (:paymentKey, :tossOrderId, :amount, :memberEmail, 'CONFIRMED', now(), now()) " +
            "ON CONFLICT (payment_key) DO UPDATE SET " +
            "toss_order_id = EXCLUDED.toss_order_id, amount = EXCLUDED.amount, " +
            "member_email = EXCLUDED.member_email, status = 'CONFIRMED', fail_reason = NULL, updated_at = now() " +
            "WHERE payments.status = 'CONFIRM_UNKNOWN'",
            nativeQuery = true)
    int upsertConfirmed(@Param("paymentKey") String paymentKey, @Param("tossOrderId") String tossOrderId,
                         @Param("amount") int amount, @Param("memberEmail") String memberEmail);
}
