package com.bluebottle.shop.category.repository;

import com.bluebottle.shop.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// JpaRepository 상속으로 기본 CRUD 자동 제공
// <Category, Long> = <엔티티 타입, PK 타입>
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // SELECT * FROM categories WHERE is_active = true ORDER BY sort_order ASC
    // 활성 카테고리만 정렬 순서대로 조회 (메뉴 노출용)
    List<Category> findByIsActiveTrueOrderBySortOrderAsc();
}