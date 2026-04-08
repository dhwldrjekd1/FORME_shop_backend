package com.bluebottle.shop.order.entity;

import com.bluebottle.shop.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

/**
 * 주문 상세 엔티티
 * - 주문(Orders)과 N:1 관계 (주문 하나에 여러 상품)
 * - 상품(Product)과 N:1 관계
 * - 주문 당시 단가(unit_price) 스냅샷 보존
 *   → 나중에 상품 가격이 변경되어도 주문 당시 가격 유지
 * - 테이블명: order_items
 */
@Entity
@Table(name = "order_items")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 주문에 속하는지
    // FetchType.LAZY: 실제 사용할 때만 DB 조회
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders orders;

    // 주문한 상품
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;      // 주문 수량

    @Column(nullable = false)
    private Integer unitPrice;
    // 주문 당시 상품 단가 스냅샷
    // 상품 가격이 나중에 바뀌어도 주문 당시 가격이 여기 저장됨
    // 총 금액 = unitPrice * quantity 로 계산
}