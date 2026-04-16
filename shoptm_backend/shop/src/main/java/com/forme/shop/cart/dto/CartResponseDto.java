package com.forme.shop.cart.dto;

import com.forme.shop.cart.entity.Cart;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

// 클라이언트에게 장바구니 정보를 응답할 때 사용하는 DTO
@Getter
@Builder
public class CartResponseDto {

    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer productPrice;   // 할인 적용된 단가
    private Integer originalPrice;  // 정가
    private Integer discountRate;   // 할인율
    private Integer quantity;
    private String size;            // 선택한 사이즈
    private Integer totalPrice;     // 단가 * 수량
    private LocalDateTime createdAt;

    // Cart 엔티티를 CartResponseDto 로 변환하는 정적 메서드
    public static CartResponseDto from(Cart cart) {
        int origPrice = cart.getProduct().getPrice();
        int discount = cart.getProduct().getDiscountRate() != null ? cart.getProduct().getDiscountRate() : 0;
        int salePrice = discount > 0 ? (int)(origPrice * (100 - discount) / 100.0) : origPrice;

        return CartResponseDto.builder()
                .id(cart.getId())
                .productId(cart.getProduct().getId())
                .productName(cart.getProduct().getName())
                .productImageUrl(cart.getProduct().getImageUrl())
                .productPrice(salePrice)
                .originalPrice(origPrice)
                .discountRate(discount)
                .quantity(cart.getQuantity())
                .size(cart.getSize())
                .totalPrice(salePrice * cart.getQuantity())
                .createdAt(cart.getCreatedAt())
                .build();
    }
}