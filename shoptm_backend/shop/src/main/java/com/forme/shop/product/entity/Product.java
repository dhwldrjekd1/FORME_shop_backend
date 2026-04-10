package com.forme.shop.product.entity;

import com.forme.shop.category.entity.Category;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * 상품 엔티티
 * - 카테고리(Category)와 N:1 관계
 * - 메인 페이지 자동 노출 플래그:
 *   is_new(신상품 4건) / is_best(베스트 4건) / is_recommend(추천 3건)
 * - 이미지 업로드: MultipartFile → 서버 저장 후 image_url 에 경로 저장
 * - 소프트 삭제: is_active = false
 * - 테이블명: products
 */
@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카테고리와 N:1 관계
    // FetchType.LAZY: 실제 사용할 때만 DB 조회 (성능 최적화)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;           // 상품명

    @Column(columnDefinition = "TEXT")
    private String description;    // 상품 설명

    @Column(nullable = false)
    private Integer price;         // 가격 (원 단위, 0 이상)

    @Builder.Default
    @Column(nullable = false)
    private Integer stock = 0;     // 재고 수량 (0 = 품절)

    @Column(name = "image_url", length = 500)
    private String imageUrl;
    // 대표 이미지 경로 (목록/썸네일용)

    @Column(name = "image_urls", length = 2000)
    private String imageUrls;
    // 다중 이미지 경로 (콤마 구분)
    // ex) /uploads/products/a.jpg,/uploads/products/b.jpg,/uploads/products/c.jpg

    @Column(length = 50)
    private String size;           // 사이즈 (S, M, L, XL, FREE 등)

    @Column(length = 10)
    private String gender;         // 성별 (남성, 여성, 공용)

    @Column(length = 50)
    private String brand;          // 브랜드 (BEANPOLE, CARHARTT, LEVI'S, DICKIES)

    @Column
    private Integer discountRate;   // 할인율 (% 단위, null이면 할인 없음)

    @Column
    private Integer originalPrice;  // 원래 가격 (할인 전)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isNew = false;
    // true = 신상품 → 메인 페이지 신상품 섹션 최신순 4건 자동 노출

    @Builder.Default
    @Column(nullable = false)
    private Boolean isBest = false;
    // true = 베스트 → 메인 페이지 베스트 섹션 최신순 4건 자동 노출

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRecommend = false;
    // true = 추천 → 메인 페이지 추천 섹션 최신순 3건 자동 노출

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;
    // true  = 판매중
    // false = 삭제된 상품 (소프트 삭제)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}