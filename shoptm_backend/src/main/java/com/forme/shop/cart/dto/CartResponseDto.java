package com.forme.shop.cart.dto;

import com.forme.shop.cart.entity.Cart;
import com.forme.shop.common.util.PriceUtil;
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
    // salePrice * quantity를 int끼리 곱하면, 상품 가격 상한(1억원, ProductRequestDto 참고)과
    // 장바구니 수량 상한(999, CartRequestDto 참고)이 겹칠 때 int 범위를 넘겨 조용히
    // 오버플로할 수 있다(OrderService.createOrder의 subtotal 계산에서 고친 것과 같은 종류의
    // 문제가 이 표시용 DTO에는 남아있었음 — 교차검증에서 발견). Long으로 바꿔 그 여지를 없앤다.
    private Long totalPrice;        // 단가 * 수량
    private LocalDateTime createdAt;

    // Cart 엔티티를 CartResponseDto 로 변환하는 정적 메서드
    public static CartResponseDto from(Cart cart) {
        int origPrice = cart.getProduct().getPrice();
        int discount = cart.getProduct().getDiscountRate() != null ? cart.getProduct().getDiscountRate() : 0;
        int salePrice = PriceUtil.applyDiscount(origPrice, discount);

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
                .totalPrice(PriceUtil.multiply(salePrice, cart.getQuantity()))
                .createdAt(cart.getCreatedAt())
                .build();
    }
}