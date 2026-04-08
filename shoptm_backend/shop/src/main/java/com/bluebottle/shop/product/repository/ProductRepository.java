package com.bluebottle.shop.product.repository;

import com.bluebottle.shop.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // SELECT * FROM products WHERE is_active = true
    // 삭제되지 않은 전체 상품 목록 조회
    List<Product> findByIsActiveTrue();

    // SELECT * FROM products WHERE category_id = ? AND is_active = true
    // 카테고리별 상품 목록 조회
    List<Product> findByCategoryIdAndIsActiveTrue(Long categoryId);

    // SELECT * FROM products WHERE name LIKE %keyword% AND is_active = true
    // 상품명으로 검색
    List<Product> findByNameContainingAndIsActiveTrue(String keyword);

    // SELECT * FROM products WHERE is_new = true AND is_active = true ORDER BY created_at DESC LIMIT 4
    // 메인 페이지 신상품 4건
    List<Product> findTop4ByIsNewTrueAndIsActiveTrueOrderByCreatedAtDesc();

    // SELECT * FROM products WHERE is_best = true AND is_active = true ORDER BY created_at DESC LIMIT 4
    // 메인 페이지 베스트 4건
    List<Product> findTop4ByIsBestTrueAndIsActiveTrueOrderByCreatedAtDesc();

    // SELECT * FROM products WHERE is_recommend = true AND is_active = true ORDER BY created_at DESC LIMIT 3
    // 메인 페이지 추천 3건
    List<Product> findTop3ByIsRecommendTrueAndIsActiveTrueOrderByCreatedAtDesc();
}