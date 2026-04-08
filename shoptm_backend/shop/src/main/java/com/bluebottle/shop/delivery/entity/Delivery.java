package com.bluebottle.shop.delivery.entity;

import com.bluebottle.shop.order.entity.Orders;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * 배송 엔티티
 * - 주문(Orders)과 1:1 관계 (주문 하나에 배송 하나)
 * - 주문 생성 시 배송 정보도 함께 생성
 * - 배송 상태 흐름:
 *   READY(배송준비) → IN_TRANSIT(배송중) → OUT_FOR_DELIVERY(배달중) → DELIVERED(배송완료)
 * - 테이블명: deliveries
 */
@Entity
@Table(name = "deliveries")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문과 1:1 관계
    // UNIQUE: 주문 하나에 배송 하나만 가능
    // FetchType.LAZY: 실제 사용할 때만 DB 조회
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Orders orders;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String carrier = "CJ대한통운";
    // 택배사명 (ex: CJ대한통운, 한진택배, 롯데택배)

    @Column(length = 50)
    private String trackingNumber;
    // 운송장 번호 (발송 전 NULL, 발송 후 입력)

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "READY";
    // 배송 상태
    // READY            = 배송 준비중 (운송장 미입력)
    // IN_TRANSIT       = 배송중 (운송장 입력 후)
    // OUT_FOR_DELIVERY = 배달중 (목적지 근처)
    // DELIVERED        = 배송 완료

    @Column
    private LocalDateTime shippedAt;
    // 실제 발송 시간 (배송 시작 전 NULL)

    @Column
    private LocalDateTime deliveredAt;
    // 배송 완료 시간 (배송 완료 전 NULL)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}