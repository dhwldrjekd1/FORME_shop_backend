package com.forme.shop.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_sizes")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 20)
    private String size;       // XS, S, M, L, XL, XXL, 28, 30 등

    @Builder.Default
    @Column(nullable = false)
    private Integer stock = 0; // 사이즈별 재고
}
