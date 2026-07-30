package com.forme.shop.review.repository;

import com.forme.shop.review.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // member/product 는 LAZY 관계인데 ReviewResponseDto.from()이 매 리뷰마다
    // member.getName()/product.getName()을 읽어서, EntityGraph 없이는 목록 조회마다 N+1이 발생함
    // SELECT * FROM reviews WHERE product_id = ? AND is_active = true ORDER BY created_at DESC
    // 특정 상품의 활성 리뷰 목록 최신순 조회
    @EntityGraph(attributePaths = {"member", "product"})
    List<Review> findByProductIdAndIsActiveTrueOrderByCreatedAtDesc(Long productId);

    // SELECT * FROM reviews WHERE member_id = ? AND is_active = true ORDER BY created_at DESC
    // 특정 회원이 작성한 활성 리뷰 목록 최신순 조회
    @EntityGraph(attributePaths = {"member", "product"})
    List<Review> findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(Long memberId);

    // 관리자 - 전체 리뷰 목록 조회 (findAll()은 EntityGraph를 붙일 수 없어 별도 메서드로 분리)
    @EntityGraph(attributePaths = {"member", "product"})
    List<Review> findAllByOrderByCreatedAtDesc();

    // SELECT COUNT(*) > 0 FROM reviews WHERE member_id = ? AND order_id = ? AND product_id = ?
    // 이미 리뷰를 작성했는지 확인 (중복 방지)
    boolean existsByMemberIdAndOrdersIdAndProductId(Long memberId, Long ordersId, Long productId);
}