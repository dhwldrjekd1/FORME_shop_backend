package com.forme.shop.payment.repository;

import com.forme.shop.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentKey(String paymentKey);

    // 이 결제를 "지금 이 요청이" 주문 생성에 쓰겠다고 선점(claim)한다.
    // CONFIRMED 상태일 때만 PROCESSING으로 바꾸는 조건부 원자적 UPDATE라서, 같은 paymentKey로
    // 동시에 여러 요청이 들어와도(더블클릭, 네트워크 재시도) 단 하나만 1을 반환하며 성공한다.
    // (재고 차감의 decreaseStockIfAvailable과 동일한 패턴)
    // 반환값(영향받은 행 수)이 0이면 이미 다른 요청이 선점했거나, 이미 주문으로 연결됐거나, 취소된 결제.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Payment p SET p.status = 'PROCESSING' WHERE p.paymentKey = :paymentKey AND p.status = 'CONFIRMED'")
    int claimIfConfirmed(@Param("paymentKey") String paymentKey);
}
