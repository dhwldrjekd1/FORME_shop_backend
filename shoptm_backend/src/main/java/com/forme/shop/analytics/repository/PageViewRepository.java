package com.forme.shop.analytics.repository;

import com.forme.shop.analytics.entity.PageView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PageViewRepository extends JpaRepository<PageView, Integer> {

    // admin 페이지 제외
    @Query("SELECT p.pageName, AVG(p.duration), COUNT(p) FROM PageView p WHERE p.pagePath NOT LIKE '/admin%' GROUP BY p.pageName ORDER BY AVG(p.duration) DESC")
    List<Object[]> findPageStats();

    @Query("SELECT p.loginId, COUNT(p), AVG(p.duration) FROM PageView p WHERE p.loginId IS NOT NULL AND p.pagePath NOT LIKE '/admin%' GROUP BY p.loginId ORDER BY COUNT(p) DESC")
    List<Object[]> findUserStats();

    @Query(value = "SELECT EXTRACT(HOUR FROM created_at) AS hour, COUNT(*) AS cnt FROM page_views WHERE page_path NOT LIKE '/admin%' GROUP BY hour ORDER BY hour", nativeQuery = true)
    List<Object[]> findHourlyStats();

    // 상품 상세 페이지별 체류시간 (product-detail만)
    @Query("SELECT p.pagePath, AVG(p.duration), COUNT(p) FROM PageView p WHERE p.pageName = 'product-detail' GROUP BY p.pagePath ORDER BY AVG(p.duration) DESC")
    List<Object[]> findProductDetailStats();

    List<PageView> findTop50ByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(DISTINCT p.pageName) FROM PageView p WHERE p.pagePath NOT LIKE '/admin%'")
    Long countDistinctPages();

    @Query("SELECT COUNT(DISTINCT p.loginId) FROM PageView p WHERE p.loginId IS NOT NULL AND p.pagePath NOT LIKE '/admin%'")
    Long countDistinctUsers();

    @Query("SELECT COALESCE(AVG(p.duration), 0) FROM PageView p WHERE p.pagePath NOT LIKE '/admin%'")
    Double avgDuration();
}
