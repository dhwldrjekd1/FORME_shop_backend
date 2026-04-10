package com.forme.shop.admin.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

// 관리자 대시보드 응답 DTO
// 매출, 회원, 주문, 상품 통계를 한번에 반환
@Getter
@Builder
public class DashboardResponseDto {

    // 회원 통계
    private Long totalMembers;        // 전체 회원 수
    private Long activeMembers;       // 활성 회원 수
    private Long bannedMembers;       // 탈퇴/강퇴 회원 수

    // 상품 통계
    private Long totalProducts;       // 전체 상품 수
    private Long onSaleProducts;      // 판매중 상품 수
    private Long soldOutProducts;     // 품절 상품 수

    // 주문 통계
    private Long totalOrders;         // 전체 주문 수
    private Long paidOrders;          // 결제완료 주문 수
    private Long shippingOrders;      // 배송중 주문 수
    private Long deliveredOrders;     // 배송완료 주문 수
    private Long cancelledOrders;     // 취소 주문 수

    // 매출 통계
    // totalPrice 가 Integer 이므로 Integer 로 변경
    private Integer totalRevenue;     // 전체 매출액

    // 최근 주문 목록 (5건)
    private List<RecentOrderDto> recentOrders;

    // 최근 주문 간략 정보
    @Getter
    @Builder
    public static class RecentOrderDto {
        private Long orderId;
        private String memberName;    // 주문자 이름
        private Integer totalPrice;   // 주문 금액 (Integer 로 변경)
        private String status;        // 주문 상태
    }
}