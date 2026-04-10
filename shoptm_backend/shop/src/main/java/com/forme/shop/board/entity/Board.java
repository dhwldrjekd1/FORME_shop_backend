package com.forme.shop.board.entity;

import com.forme.shop.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * 게시판 엔티티 (자유게시판)
 * - 회원(Member)과 N:1 관계 (한 회원이 여러 게시글 작성 가능)
 * - 댓글(Comment)과 1:N 관계
 * - 조회수(views) 게시글 조회 시 자동 증가
 * - 소프트 삭제: is_active = false
 * - 테이블명: boards
 */
@Entity
@Table(name = "boards")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 게시글 작성자
    // FetchType.LAZY: 실제 사용할 때만 DB 조회
    // ON DELETE CASCADE: 회원 탈퇴 시 게시글도 자동 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 200)
    private String title;          // 게시글 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;        // 게시글 내용

    @Builder.Default
    @Column(nullable = false)
    private Integer views = 0;
    // 조회수 (게시글 상세 조회 시 +1 증가)
    // 기본값 0

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;
    // true  = 정상 게시글
    // false = 삭제된 게시글 (소프트 삭제)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}