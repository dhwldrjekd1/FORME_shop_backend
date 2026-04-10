package com.forme.shop.qna.repository;

import com.forme.shop.qna.entity.Qna;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QnaRepository extends JpaRepository<Qna, Long> {

    // SELECT * FROM qna WHERE is_active = true ORDER BY created_at DESC
    // 삭제되지 않은 전체 Q&A 최신순 조회
    List<Qna> findByIsActiveTrueOrderByCreatedAtDesc();

    // SELECT * FROM qna WHERE member_id = ? AND is_active = true ORDER BY created_at DESC
    // 특정 회원의 Q&A 최신순 조회
    List<Qna> findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(Long memberId);

    // SELECT * FROM qna WHERE product_id = ? AND is_active = true ORDER BY created_at DESC
    // 특정 상품의 Q&A 최신순 조회
    List<Qna> findByProductIdAndIsActiveTrueOrderByCreatedAtDesc(Long productId);

    // SELECT * FROM qna WHERE status = ? AND is_active = true
    // 관리자 - 미답변 Q&A 목록 조회
    List<Qna> findByStatusAndIsActiveTrue(String status);
}