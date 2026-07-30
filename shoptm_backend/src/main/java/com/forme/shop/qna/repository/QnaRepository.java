package com.forme.shop.qna.repository;

import com.forme.shop.qna.entity.Qna;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QnaRepository extends JpaRepository<Qna, Long> {

    // member/product 는 LAZY 관계인데 QnaResponseDto.from()이 매 Q&A마다
    // member.getName()/product.getName()을 읽어서, EntityGraph 없이는 목록 조회마다 N+1이 발생함
    // SELECT * FROM qna WHERE is_active = true ORDER BY created_at DESC
    // 삭제되지 않은 전체 Q&A 최신순 조회
    @EntityGraph(attributePaths = {"member", "product"})
    List<Qna> findByIsActiveTrueOrderByCreatedAtDesc();

    // SELECT * FROM qna WHERE member_id = ? AND is_active = true ORDER BY created_at DESC
    // 특정 회원의 Q&A 최신순 조회
    @EntityGraph(attributePaths = {"member", "product"})
    List<Qna> findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(Long memberId);

    // SELECT * FROM qna WHERE product_id = ? AND is_active = true ORDER BY created_at DESC
    // 특정 상품의 Q&A 최신순 조회
    @EntityGraph(attributePaths = {"member", "product"})
    List<Qna> findByProductIdAndIsActiveTrueOrderByCreatedAtDesc(Long productId);

    // SELECT * FROM qna WHERE status = ? AND is_active = true
    // 관리자 - 미답변 Q&A 목록 조회
    @EntityGraph(attributePaths = {"member", "product"})
    List<Qna> findByStatusAndIsActiveTrue(String status);
}