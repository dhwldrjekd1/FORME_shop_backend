package com.forme.shop.review.entity;

import com.forme.shop.member.entity.Member;
import com.forme.shop.order.entity.Orders;
import com.forme.shop.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * 리뷰 엔티티
 * - 구매한 내역(order_id)이 있는 경우에만 리뷰 작성 가능
 * - 동일 회원이 동일 주문의 동일 상품에 중복 리뷰 방지
 *   UNIQUE(member_id, order_id, product_id)
 * - 관리자 소프트 삭제: is_active = false
 * - 테이블명: reviews
 */
@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "order_id", "product_id"}))
// 주문 1건당 상품 1개 리뷰 제한
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 리뷰 작성자
    // ON DELETE CASCADE: 회원 탈퇴 시 리뷰도 자동 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 리뷰 대상 상품
    // ON DELETE CASCADE: 상품 삭제 시 리뷰도 자동 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 구매 확인용 주문 참조
    // 이 주문을 통해 실제로 구매했는지 검증할 때 사용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders orders;

    @Column(nullable = false)
    private Integer rating;
    // 별점 (1 ~ 5)
    // DB: CHECK (rating BETWEEN 1 AND 5) 제약 적용

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;        // 리뷰 내용

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;
    // true  = 정상 리뷰
    // false = 관리자가 삭제한 리뷰 (소프트 삭제)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}