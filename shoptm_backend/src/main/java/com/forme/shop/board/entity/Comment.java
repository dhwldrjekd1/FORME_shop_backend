package com.forme.shop.board.entity;

import com.forme.shop.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * 댓글 엔티티
 * - 게시글(Board)과 N:1 관계 (게시글 하나에 여러 댓글 가능)
 * - 회원(Member)과 N:1 관계 (한 회원이 여러 댓글 작성 가능)
 * - 소프트 삭제: is_active = false
 * - 테이블명: comments
 */
@Entity
@Table(name = "comments")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 댓글이 달린 게시글
    // FetchType.LAZY: 실제 사용할 때만 DB 조회
    // ON DELETE CASCADE: 게시글 삭제 시 댓글도 자동 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    // 댓글 작성자
    // ON DELETE CASCADE: 회원 탈퇴 시 댓글도 자동 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;        // 댓글 내용

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;
    // true  = 정상 댓글
    // false = 삭제된 댓글 (소프트 삭제)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}