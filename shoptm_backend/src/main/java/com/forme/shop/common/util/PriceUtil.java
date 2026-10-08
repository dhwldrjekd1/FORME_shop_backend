package com.forme.shop.common.util;

// 상품 가격에 할인율을 적용하는 계산을 한 곳에 모은 것. CartResponseDto(장바구니 화면에
// 보여주는 단가)와 OrderService(그 금액을 실제로 결제 검증하는 쪽) 양쪽이 각자 같은 식을
// 복사해 쓰고 있었는데, 한쪽만 Math.round(반올림)를 쓰고 다른 쪽은 소수점을 버리는 방식을
// 써서 할인율 적용 시 소수점이 .5 이상인 가격(예: 103원의 15% 할인 = 87.55원)에서 두 계산이
// 서로 다른 값을 냈다 — 장바구니/결제 화면은 87원으로 보여주고 실제로 그 금액을 토스에
// 청구하는데, 주문 생성 쪽은 88원을 기대해서 "결제 금액과 주문 금액이 일치하지 않습니다"로
// 주문이 실패(이미 결제된 금액은 자동 환불)하는 사고로 이어질 수 있었다. 앞으로 이 계산을
// 쓰는 곳은 전부 이 메서드 하나만 쓰도록 해서, 두 곳이 또 각자 복사되다 다시 어긋나는 것을 막는다.
public class PriceUtil {

    private PriceUtil() {}

    // 할인율(discountRate)이 null/0 이하이면 원가 그대로, 아니면 소수점을 버린(반올림 아님)
    // 할인가를 반환한다.
    public static int applyDiscount(int price, Integer discountRate) {
        if (discountRate == null || discountRate <= 0) return price;
        return (int) (price * (100 - discountRate) / 100.0);
    }
}
