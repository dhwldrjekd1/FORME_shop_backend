package com.forme.shop.review.repository;

import com.forme.shop.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // SELECT * FROM reviews WHERE product_id = ? AND is_active = true ORDER BY created_at DESC
    // 특정 상품의 활성 리뷰 목록 최신순 조회
    List<Review> findByProductIdAndIsActiveTrueOrderByCreatedAtDesc(Long productId);

    // SELECT * FROM reviews WHERE member_id = ? AND is_active = true ORDER BY created_at DESC
    // 특정 회원이 작성한 활성 리뷰 목록 최신순 조회
    List<Review> findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(Long memberId);

    // SELECT COUNT(*) > 0 FROM reviews WHERE member_id = ? AND order_id = ? AND product_id = ?
    // 이미 리뷰를 작성했는지 확인 (중복 방지)
    boolean existsByMemberIdAndOrdersIdAndProductId(Long memberId, Long ordersId, Long productId);
}