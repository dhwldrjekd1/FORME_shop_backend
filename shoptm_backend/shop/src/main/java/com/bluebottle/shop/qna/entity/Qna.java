package com.bluebottle.shop.qna.entity;

import com.bluebottle.shop.member.entity.Member;
import com.bluebottle.shop.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Q&A 엔티티
 * - 상품 문의 (product_id 있음) / 일반 문의 (product_id NULL) 두 가지 형태
 * - 비밀글 기능: is_secret = true 이면 작성자와 관리자만 조회 가능
 * - 답변 상태: PENDING(미답변) / ANSWERED(답변완료)
 * - 소프트 삭제: is_active = false
 * - 테이블명: qna
 */
@Entity
@Table(name = "qna")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Qna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 질문 작성자
    // ON DELETE CASCADE: 회원 탈퇴 시 Q&A도 자동 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 질문 대상 상품 (NULL 허용 → 일반 문의는 상품 없이 가능)
    // ON DELETE SET NULL: 상품 삭제 시 product_id 를 NULL 로 변경
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // 답변한 관리자
    // NULL = 아직 답변 안 함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by")
    private Member answeredBy;

    @Column(nullable = false, length = 200)
    private String title;          // 질문 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;        // 질문 내용

    @Column(columnDefinition = "TEXT")
    private String answer;         // 관리자 답변 (NULL = 미답변)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isSecret = false;
    // true  = 비밀글 (작성자와 관리자만 열람 가능)
    // false = 공개글

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;
    // true  = 정상
    // false = 삭제된 Q&A (소프트 삭제)

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PENDING";
    // PENDING  = 미답변 (답변 대기중)
    // ANSWERED = 답변 완료

    @Column
    private LocalDateTime answeredAt;  // 답변 등록 시간 (미답변 시 NULL)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}