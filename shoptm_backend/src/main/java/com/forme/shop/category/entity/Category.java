package com.forme.shop.category.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 상품 카테고리 엔티티
 * - 상품(Product)은 반드시 하나의 카테고리에 속함
 * - ex) 원두, 드립 커피, 장비 & 도구, 굿즈, 구독 패키지
 * - sort_order 로 카테고리 노출 순서 조절
 * - 테이블명: categories
 */
@Entity
@Table(name = "categories")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;           // 카테고리명 (ex: 원두, 굿즈)

    @Column(length = 200)
    private String description;    // 카테고리 설명 (선택)

    @Builder.Default
    @Column(nullable = false)
    private Integer sortOrder = 0;
    // 카테고리 정렬 순서
    // 숫자가 작을수록 먼저 노출 (ex: 1=원두, 2=드립커피)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;
    // true  = 활성 카테고리 (노출)
    // false = 비활성 카테고리 (숨김)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}