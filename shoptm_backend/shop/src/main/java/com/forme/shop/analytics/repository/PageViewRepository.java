package com.forme.shop.analytics.repository;

import com.forme.shop.analytics.entity.PageView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PageViewRepository extends JpaRepository<PageView, Integer> {

    @Query("SELECT p.pageName, AVG(p.duration), COUNT(p) FROM PageView p GROUP BY p.pageName ORDER BY AVG(p.duration) DESC")
    List<Object[]> findPageStats();

    @Query("SELECT p.loginId, COUNT(p), AVG(p.duration) FROM PageView p WHERE p.loginId IS NOT NULL GROUP BY p.loginId ORDER BY COUNT(p) DESC")
    List<Object[]> findUserStats();

    @Query(value = "SELECT EXTRACT(HOUR FROM created_at) AS hour, COUNT(*) AS cnt FROM page_views GROUP BY hour ORDER BY hour", nativeQuery = true)
    List<Object[]> findHourlyStats();

    List<PageView> findTop50ByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(DISTINCT p.pageName) FROM PageView p")
    Long countDistinctPages();

    @Query("SELECT COUNT(DISTINCT p.loginId) FROM PageView p WHERE p.loginId IS NOT NULL")
    Long countDistinctUsers();

    @Query("SELECT COALESCE(AVG(p.duration), 0) FROM PageView p")
    Double avgDuration();
}
