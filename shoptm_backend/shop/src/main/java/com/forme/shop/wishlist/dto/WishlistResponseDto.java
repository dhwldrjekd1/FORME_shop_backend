package com.forme.shop.wishlist.dto;

import com.forme.shop.wishlist.entity.Wishlist;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WishlistResponseDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer productPrice;
    private String brand;
    private String category;
    private Integer discountRate;

    public static WishlistResponseDto from(Wishlist w) {
        return WishlistResponseDto.builder()
                .id(w.getId())
                .productId(w.getProduct().getId())
                .productName(w.getProduct().getName())
                .productImageUrl(w.getProduct().getImageUrl())
                .productPrice(w.getProduct().getPrice())
                .brand(w.getProduct().getBrand())
                .category(w.getProduct().getCategory() != null ? w.getProduct().getCategory().getName() : "")
                .discountRate(w.getProduct().getDiscountRate())
                .build();
    }
}
