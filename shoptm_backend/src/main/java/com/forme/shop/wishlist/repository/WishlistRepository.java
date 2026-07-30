package com.forme.shop.wishlist.repository;

import com.forme.shop.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    // product/product.category는 LAZY 관계인데 WishlistResponseDto.from()이 매 항목마다
    // product의 여러 필드와 product.category.getName()을 읽어서, EntityGraph 없이는
    // 목록 조회마다 항목당 최대 2개의 추가 쿼리(N+1)가 발생함
    @EntityGraph(attributePaths = {"product", "product.category"})
    List<Wishlist> findByMemberId(Long memberId);
    Optional<Wishlist> findByMemberIdAndProductId(Long memberId, Long productId);
    boolean existsByMemberIdAndProductId(Long memberId, Long productId);
    void deleteByMemberIdAndProductId(Long memberId, Long productId);
}
