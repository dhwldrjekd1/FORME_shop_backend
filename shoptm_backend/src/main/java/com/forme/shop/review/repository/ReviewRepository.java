package com.forme.shop.review.repository;

import com.forme.shop.review.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

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

    // createReview()가 원자적 삽입 직후 응답 조립에 쓰는 단건 조회라 위와 같은 이유로 EntityGraph 필요
    @EntityGraph(attributePaths = {"member", "product"})
    Optional<Review> findByMemberIdAndProductId(Long memberId, Long productId);

    // 이미 리뷰가 있으면 삽입하지 않는(DO NOTHING) 원자적 삽입. "확인 후 insert" 방식은 두 요청이
    // 동시에 들어오면(연속 클릭 등) 둘 다 "아직 없음"으로 보고 둘 다 insert를 시도해 DB 제약
    // 위반으로 500이 날 수 있었음(장바구니/찜과 같은 이유). 반환값(영향받은 행 수)이 0이면
    // 이미 리뷰가 있었다는 뜻이므로 호출 쪽에서 "이미 리뷰를 작성했습니다" 에러로 안내한다.
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO reviews (member_id, product_id, order_id, rating, content, is_active, created_at, updated_at) " +
            "VALUES (:memberId, :productId, :orderId, :rating, :content, true, now(), now()) " +
            "ON CONFLICT (member_id, product_id) DO NOTHING",
            nativeQuery = true)
    int insertReviewIfAbsent(@Param("memberId") Long memberId, @Param("productId") Long productId,
                              @Param("orderId") Long orderId, @Param("rating") Integer rating,
                              @Param("content") String content);
}