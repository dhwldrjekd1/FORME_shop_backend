package com.bluebottle.shop.order.entity;

import com.bluebottle.shop.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 엔티티
 * - 회원(Member)과 N:1 관계 (한 회원이 여러 주문 가능)
 * - 주문 상세(OrderItem)와 1:N 관계
 * - 주문 상태 흐름:
 *   PENDING(주문대기) → PAID(결제완료) → PREPARING(상품준비중)
 *   → SHIPPED(배송중) → DELIVERED(배송완료) / CANCELLED(취소)
 * - 테이블명: orders (order 는 SQL 예약어라 orders 사용)
 */
@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문한 회원
    // FetchType.LAZY: 실제 사용할 때만 DB 조회
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 주문 상세 목록 (1:N)
    // CascadeType.ALL: 주문 저장/삭제 시 주문 상세도 함께 처리
    // orphanRemoval = true: 주문에서 제거된 상세 항목은 DB에서도 삭제
    @Builder.Default
    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(nullable = false)
    private Integer totalPrice;
    // 총 결제 금액 (주문 당시 가격 기준으로 계산)
    // 상품 가격이 나중에 변경되어도 주문 당시 금액 유지

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "PENDING";
    // 주문 상태
    // PENDING   = 주문 대기 (결제 전)
    // PAID      = 결제 완료
    // PREPARING = 상품 준비중
    // SHIPPED   = 배송중
    // DELIVERED = 배송 완료
    // CANCELLED = 주문 취소

    @Column(length = 50, nullable = false)
    private String receiverName;   // 수령인 이름

    @Column(length = 20, nullable = false)
    private String receiverPhone;  // 수령인 전화번호

    @Column(nullable = false)
    private String address;        // 배송 주소

    @Column(length = 255)
    private String memo;           // 배송 메모 (ex: 문 앞에 놓아주세요)

    @Column
    private LocalDateTime paidAt;  // 결제 완료 시간 (결제 전은 NULL)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}