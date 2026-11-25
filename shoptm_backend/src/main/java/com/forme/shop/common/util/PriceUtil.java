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
        // price * (100 - discountRate)를 int끼리 먼저 곱하면 /100.0으로 double 승격되기도
        // 전에 이미 그 곱셈 자체에서 int 범위를 넘겨 조용히 오버플로할 수 있다 — ProductRequestDto
        // 의 가격 상한(1억원)만으로는 할인율이 조금만 걸려도(예: 10%면 약 2170만원부터) 이
        // 오버플로를 막기에 부족하다(교차검증에서 발견). price를 미리 double로 바꿔 곱셈
        // 자체를 double 연산으로 만든다 — 결과값 자체는 항상 입력 price 이하라 int로 다시
        // 좁혀도 범위를 넘을 일이 없다.
        return (int) ((double) price * (100 - discountRate) / 100.0);
    }

    // 단가 * 수량을 계산하는 모든 곳(CartResponseDto/OrderService/OrderResponseDto/AdminService
    // 등)이 각자 int끼리 곱해서, 상품 가격 상한(ProductRequestDto.MAX_PRICE, 1억원)과 수량
    // 상한(999)이 겹치면 조용히 int 범위를 넘겨 음수 등 틀린 값으로 뒤집히는 문제가 여러 곳에
    // 따로 있었다(다른 방향에서 버그를 찾다가 발견 — 이 계산이 한 곳에 모여있지 않아서, 한 곳을
    // 고치고도 같은 패턴의 다른 호출부를 놓치는 일이 반복됐음). 이후로 단가*수량을 계산하는
    // 곳은 전부 이 메서드 하나만 쓰도록 해서, PriceUtil 클래스 설명에 적힌 것과 같은 종류의
    // "계산식이 복사되다 어긋나는" 사고가 이 패턴에서도 반복되지 않게 한다. 결과를 long으로
    // 돌려주는 이유는 호출하는 쪽이 그 값을 다른 값과 더 합산하거나(여러 품목의 합계 등) 범위를
    // 확인하기 전까지는 int로 너무 일찍 좁히면 안 되기 때문이다.
    public static long multiply(int unitPrice, int quantity) {
        return (long) unitPrice * quantity;
    }
}
