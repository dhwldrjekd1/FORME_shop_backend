package com.forme.shop.wishlist.repository;

import com.forme.shop.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    // product/product.category는 LAZY 관계인데 WishlistResponseDto.from()이 매 항목마다
    // product의 여러 필드와 product.category.getName()을 읽어서, EntityGraph 없이는
    // 목록 조회마다 항목당 최대 2개의 추가 쿼리(N+1)가 발생함
    @EntityGraph(attributePaths = {"product", "product.category"})
    List<Wishlist> findByMemberId(Long memberId);

    // addWishlist()가 upsert 직후 응답 조립에 쓰는 단건 조회라 위와 같은 이유로 EntityGraph 필요
    @EntityGraph(attributePaths = {"product", "product.category"})
    Optional<Wishlist> findByMemberIdAndProductId(Long memberId, Long productId);
    boolean existsByMemberIdAndProductId(Long memberId, Long productId);
    void deleteByMemberIdAndProductId(Long memberId, Long productId);

    // 이미 찜한 상품이면 조용히 무시하고(DO NOTHING) 없으면 새로 담는 원자적 upsert.
    // "확인 후 없으면 insert" 방식은 같은 상품을 동시에 두 번 찜하면(하트 연타 등) 둘 다
    // "없음"으로 보고 둘 다 insert를 시도해 DB unique 제약 위반으로 500이 날 수 있었음
    // (장바구니 담기에서 겪은 것과 같은 종류의 버그 — 같은 방식으로 해결)
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO wishlists (member_id, product_id, created_at) " +
            "VALUES (:memberId, :productId, now()) " +
            "ON CONFLICT (member_id, product_id) DO NOTHING",
            nativeQuery = true)
    void upsertWishlist(@Param("memberId") Long memberId, @Param("productId") Long productId);
}
