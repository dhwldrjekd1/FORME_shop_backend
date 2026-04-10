package com.forme.shop.cart.entity;

import com.forme.shop.member.entity.Member;
import com.forme.shop.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * 장바구니 엔티티
 * - 회원(Member)과 상품(Product)의 중간 테이블
 * - 동일 회원이 동일 상품 중복 담기 방지: UNIQUE(member_id, product_id)
 * - 이미 담긴 상품이면 수량만 추가하는 방식으로 처리
 * - 테이블명: cart_items
 */
@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "product_id"}))
// 같은 회원이 같은 상품을 중복으로 담지 못하도록 UNIQUE 제약
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 장바구니 주인 (회원)
    // FetchType.LAZY: 실제 사용할 때만 DB 조회 (성능 최적화)
    // ON DELETE CASCADE: 회원 탈퇴 시 장바구니도 자동 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 장바구니에 담긴 상품
    // ON DELETE CASCADE: 상품 삭제 시 장바구니에서도 자동 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantity = 1;  // 수량 기본값 1, 최소 1개 이상

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}