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
 * - 구매 인증(order_id)은 선택 — 있으면 본인 주문인지 검증 후 연결, 없어도 작성 가능
 * - 구매 인증 여부와 무관하게 동일 회원이 동일 상품에 중복 리뷰 방지
 *   UNIQUE(member_id, product_id)
 * - isActive는 하드 삭제 이전에 소프트 삭제용으로 쓰였던 흔적 — 지금 삭제(본인/관리자
 *   모두 ReviewService.deleteReview)는 행 자체를 지우는 하드 삭제라 실제로 false가 되는
 *   경우가 없다. isActive=true만 걸러내는 조회 조건들은 항상 참인 조건이라 동작에는
 *   영향이 없지만, 이후 다시 소프트 삭제로 바꾸지 않는 한 이 컬럼은 의미가 없다.
 * - 테이블명: reviews
 */
@Entity
@Table(name = "reviews")
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
    @JoinColumn(name = "order_id", nullable = true)
    private Orders orders;

    @Column(nullable = false)
    private Integer rating;
    // 별점 (1 ~ 5)
    // DB: CHECK (rating BETWEEN 1 AND 5) 제약 적용

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;        // 리뷰 내용

    @Column(columnDefinition = "TEXT")
    private String reply;          // 관리자 답글

    @Column
    private LocalDateTime repliedAt; // 답글 작성 시간

    // 실제로는 항상 true — 클래스 상단 설명 참고 (삭제는 하드 삭제라 false가 되지 않음)
    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}