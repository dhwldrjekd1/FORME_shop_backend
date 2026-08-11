package com.forme.shop.product.repository;

import com.forme.shop.product.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 재고가 충분할 때만 원자적으로 차감 - 조회 후 다시 쓰는(read-then-write) 방식은
    // 동시 주문 시 같은 재고를 여러 요청이 동시에 통과시켜 오버셀을 일으킬 수 있어,
    // "재고 >= 주문수량"을 DB 단에서 함께 확인하는 조건부 UPDATE로 처리한다.
    // 반환값(영향받은 행 수)이 0이면 재고 부족으로 차감이 적용되지 않은 것.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :productId AND p.stock >= :quantity")
    int decreaseStockIfAvailable(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    // 주문 취소 시 재고 복구 — 위와 같은 이유로 원자적 UPDATE로 처리한다.
    // (조회 후 다시 쓰는 방식이면, 같은 상품이 포함된 두 주문이 동시에 취소될 때 한쪽의
    // 복구분이 다른 쪽에 덮어써져 유실될 수 있음 — 오버셀 방지 원자적 UPDATE 도입 이후에도
    // 취소 경로에는 이 처리가 빠져 있던 것을 뒤늦게 발견해 추가함)
    // decreaseStockIfAvailable과 동일하게 영향받은 행 수를 반환 — 0이면(상품이 그 사이
    // 삭제되는 등) 복구가 조용히 실패한 것이므로 호출 쪽에서 로그를 남길 수 있게 함.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :productId")
    int increaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    // SELECT * FROM products WHERE is_active = true
    // 삭제되지 않은 전체 상품 목록 조회
    // category/sizes 는 LAZY 라서 DTO 변환 시 상품마다 추가 쿼리가 나가는 N+1이 발생함
    // → EntityGraph 로 한 쿼리에서 함께 조회
    @EntityGraph(attributePaths = {"category", "sizes"})
    List<Product> findByIsActiveTrue();

    // SELECT * FROM products WHERE category_id = ? AND is_active = true
    // 카테고리별 상품 목록 조회
    @EntityGraph(attributePaths = {"category", "sizes"})
    List<Product> findByCategoryIdAndIsActiveTrue(Long categoryId);

    // SELECT * FROM products WHERE name LIKE %keyword% AND is_active = true
    // 상품명으로 검색
    @EntityGraph(attributePaths = {"category", "sizes"})
    List<Product> findByNameContainingAndIsActiveTrue(String keyword);

    // SELECT * FROM products WHERE is_new = true AND is_active = true ORDER BY created_at DESC LIMIT 4
    // 메인 페이지 신상품 4건
    @EntityGraph(attributePaths = {"category", "sizes"})
    List<Product> findTop4ByIsNewTrueAndIsActiveTrueOrderByCreatedAtDesc();

    // SELECT * FROM products WHERE is_best = true AND is_active = true ORDER BY created_at DESC LIMIT 4
    // 메인 페이지 베스트 4건
    @EntityGraph(attributePaths = {"category", "sizes"})
    List<Product> findTop4ByIsBestTrueAndIsActiveTrueOrderByCreatedAtDesc();

    // SELECT * FROM products WHERE is_recommend = true AND is_active = true ORDER BY created_at DESC LIMIT 4
    // 메인 페이지 추천 4건 (브랜드당 1개)
    @EntityGraph(attributePaths = {"category", "sizes"})
    List<Product> findByIsRecommendTrueAndIsActiveTrueOrderByIdAsc();

    // 브랜드별 추천 상품 존재 여부 확인
    boolean existsByBrandAndIsRecommendTrueAndIsActiveTrue(String brand);

    // 브랜드별 추천 상품 조회 (추천 해제용)
    List<Product> findByBrandAndIsRecommendTrueAndIsActiveTrue(String brand);
}