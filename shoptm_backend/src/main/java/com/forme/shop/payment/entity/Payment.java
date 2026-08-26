package com.forme.shop.payment.entity;

import com.forme.shop.order.entity.Orders;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * 토스페이먼츠 결제 승인 기록 엔티티
 * - 결제 승인(confirm) 성공 시점에 즉시 저장해, 이후 주문 생성이 실패해도
 *   "카드는 결제됐는데 기록이 하나도 없는" 상태가 남지 않도록 함
 * - 주문(Orders)과 1:1 관계지만, 주문 생성 전(결제만 확정된) 상태에서는 orders가 NULL
 * - 결제 상태 흐름:
 *   CONFIRMED(승인완료, 주문 생성 전) → LINKED(주문 생성 성공)
 *                                    → REFUNDED(주문 생성 실패로 자동 환불됨)
 *                                    → REFUND_FAILED(환불 시도도 실패 — 수동 확인 필요)
 * - 테이블명: payments
 */
@Entity
@Table(name = "payments")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String paymentKey;
    // 토스가 발급한 결제 고유 키 (재조회/취소 API 호출 시 사용)

    @Column(nullable = false, length = 100)
    private String tossOrderId;
    // 결제 요청 시 우리 쪽에서 생성해 토스에 넘긴 주문 식별자 (FORME_타임스탬프)

    @Column(nullable = false)
    private Integer amount;
    // 토스가 실제로 승인했다고 응답한 금액

    @Column(length = 255)
    private String memberEmail;
    // 이 결제를 실제로 승인받은(=confirm을 호출한) 회원의 이메일. 주문 생성 시 이 값과
    // 대상 회원이 같은지 확인해, paymentKey만 알아내서 남의 결제로 주문을 가로채지 못하게 막는 데 쓴다
    // (OrderService.createOrder 참고). 이 컬럼 추가 이전에 생성된 기존 행은 NULL로 남아있음.

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "CONFIRMED";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Orders orders;
    // 주문 생성 성공 시에만 연결됨 (실패하면 계속 NULL로 남아 환불 대상이었음을 추적 가능)

    @Column(length = 255)
    private String failReason;
    // 주문 생성 실패로 자동 환불(또는 환불 실패)된 경우의 사유

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
