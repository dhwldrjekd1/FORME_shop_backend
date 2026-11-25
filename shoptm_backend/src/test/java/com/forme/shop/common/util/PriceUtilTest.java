package com.forme.shop.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// PriceUtil.applyDiscount()가 price*(100-discountRate)를 int끼리 먼저 곱해 오버플로하던
// 문제를 고친 뒤 검증 — ProductRequestDto의 가격 상한(1억원)만으로는 할인율이 조금만 걸려도
// (10%면 약 2170만원부터) 이 오버플로를 막기에 부족했음(다른 방향에서 버그를 찾다가 발견).
class PriceUtilTest {

    @Test
    void 할인율이_없으면_원가_그대로_반환한다() {
        assertEquals(100_000_000, PriceUtil.applyDiscount(100_000_000, null));
        assertEquals(100_000_000, PriceUtil.applyDiscount(100_000_000, 0));
    }

    @Test
    void 가격_상한_근처에서도_오버플로_없이_정확히_계산된다() {
        // 예전 구현(price * (100 - discountRate) / 100.0)이라면 100_000_000 * 90 =
        // 9_000_000_000으로 이미 int 범위(약 21억)를 넘겨 오버플로했을 값.
        assertEquals(90_000_000, PriceUtil.applyDiscount(100_000_000, 10));
        assertEquals(50_000_000, PriceUtil.applyDiscount(100_000_000, 50));
    }

    @Test
    void 소수점은_반올림이_아니라_버린다() {
        // 103원의 15% 할인 = 87.55원 → 87원(버림). CartResponseDto/OrderService가 정확히
        // 같은 기준을 쓰기 위한 핵심 동작이라 그대로 유지돼야 한다.
        assertEquals(87, PriceUtil.applyDiscount(103, 15));
    }

    @Test
    void 단가_수량_곱셈이_int_범위를_넘겨도_정확히_계산된다() {
        // 가격 상한(1억원)과 수량 상한(999)이 겹치면 int끼리 곱했을 때 약 999억으로
        // int 범위(약 21억)를 넘겨 오버플로했을 값 — CartResponseDto/OrderService/
        // OrderResponseDto/AdminService가 전부 이 메서드 하나로 통일된 이유.
        assertEquals(99_900_000_000L, PriceUtil.multiply(100_000_000, 999));
        assertEquals(0L, PriceUtil.multiply(100_000_000, 0));
    }
}
