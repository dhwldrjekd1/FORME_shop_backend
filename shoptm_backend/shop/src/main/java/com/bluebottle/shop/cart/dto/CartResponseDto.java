package com.bluebottle.shop.cart.dto;

import com.bluebottle.shop.cart.entity.Cart;
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
    private Integer productPrice;   // ✅ BigDecimal → Integer
    private Integer quantity;
    private Integer totalPrice;     // ✅ BigDecimal → Integer (가격 * 수량)
    private LocalDateTime createdAt;

    // Cart 엔티티를 CartResponseDto 로 변환하는 정적 메서드
    public static CartResponseDto from(Cart cart) {
        return CartResponseDto.builder()
                .id(cart.getId())
                .productId(cart.getProduct().getId())
                .productName(cart.getProduct().getName())
                .productImageUrl(cart.getProduct().getImageUrl())
                .productPrice(cart.getProduct().getPrice())
                .quantity(cart.getQuantity())
                // 총 금액 = 상품 가격 * 수량 (Integer 곱셈)
                .totalPrice(cart.getProduct().getPrice() * cart.getQuantity())
                .createdAt(cart.getCreatedAt())
                .build();
    }
}