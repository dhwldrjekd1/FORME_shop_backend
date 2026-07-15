package com.forme.shop.product.repository;

import com.forme.shop.product.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

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