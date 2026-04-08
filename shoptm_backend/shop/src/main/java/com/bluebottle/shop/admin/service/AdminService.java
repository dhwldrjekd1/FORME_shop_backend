package com.bluebottle.shop.admin.service;

import com.bluebottle.shop.admin.dto.DashboardResponseDto;
import com.bluebottle.shop.member.entity.Member;
import com.bluebottle.shop.member.repository.MemberRepository;
import com.bluebottle.shop.order.entity.Orders;
import com.bluebottle.shop.order.repository.OrderRepository;
import com.bluebottle.shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // 기본적으로 읽기 전용 트랜잭션 (조회 성능 최적화)
public class AdminService {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    // 관리자 대시보드 통계 조회
    // 회원/상품/주문/매출 통계를 한번에 반환
    public DashboardResponseDto getDashboard() {

        // 전체 회원 목록 한번에 조회 (반복 쿼리 방지)
        List<Member> allMembers = memberRepository.findAll();

        // 전체 주문 목록 한번에 조회 (반복 쿼리 방지)
        List<Orders> allOrders = orderRepository.findAll();

        // =====================
        // 회원 통계
        // =====================
        long totalMembers  = allMembers.size();  // 전체 회원 수

        long activeMembers = allMembers.stream()
                .filter(Member::getIsActive)     // isActive = true 인 회원만
                .count();                         // 활성 회원 수

        long bannedMembers = totalMembers - activeMembers;  // 탈퇴/강퇴 회원 수

        // =====================
        // 상품 통계
        // =====================
        long totalProducts   = productRepository.count();  // 전체 상품 수

        // isActive = true 인 상품 = 판매중 상품
        long onSaleProducts  = productRepository.findByIsActiveTrue().size();

        // isActive = true 이면서 stock = 0 인 상품 = 품절 상품
        long soldOutProducts = productRepository.findByIsActiveTrue().stream()
                .filter(p -> p.getStock() == 0)
                .count();

        // =====================
        // 주문 통계
        // =====================
        long totalOrders     = allOrders.size();  // 전체 주문 수

        long paidOrders      = allOrders.stream()
                .filter(o -> o.getStatus().equals("PAID"))
                .count();                          // 결제 완료 주문 수

        long shippingOrders  = allOrders.stream()
                .filter(o -> o.getStatus().equals("SHIPPED"))
                .count();                          // 배송중 주문 수

        long deliveredOrders = allOrders.stream()
                .filter(o -> o.getStatus().equals("DELIVERED"))
                .count();                          // 배송 완료 주문 수

        long cancelledOrders = allOrders.stream()
                .filter(o -> o.getStatus().equals("CANCELLED"))
                .count();                          // 취소 주문 수

        // =====================
        // 매출 통계
        // =====================
        // 취소된 주문 제외하고 totalPrice 합산
        // mapToInt 로 Integer 스트림으로 변환 후 sum()
        int totalRevenue = allOrders.stream()
                .filter(o -> !o.getStatus().equals("CANCELLED"))
                .mapToInt(Orders::getTotalPrice)
                .sum();

        // =====================
        // 최근 주문 5건
        // =====================
        List<DashboardResponseDto.RecentOrderDto> recentOrders =
                orderRepository.findAllByOrderByCreatedAtDesc()
                        .stream()
                        .limit(5)   // 최근 5건만 가져옴
                        .map(o -> DashboardResponseDto.RecentOrderDto.builder()
                                .orderId(o.getId())
                                .memberName(o.getMember().getName())
                                .totalPrice(o.getTotalPrice())
                                .status(o.getStatus())
                                .build())
                        .collect(Collectors.toList());

        // 통계 데이터 DTO 로 반환
        return DashboardResponseDto.builder()
                .totalMembers(totalMembers)
                .activeMembers(activeMembers)
                .bannedMembers(bannedMembers)
                .totalProducts(totalProducts)
                .onSaleProducts(onSaleProducts)
                .soldOutProducts(soldOutProducts)
                .totalOrders(totalOrders)
                .paidOrders(paidOrders)
                .shippingOrders(shippingOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .totalRevenue(totalRevenue)
                .recentOrders(recentOrders)
                .build();
    }

    // 관리자 - 회원 검색 (이름 또는 이메일로 검색)
    public List<Member> searchMembers(String keyword) {
        return memberRepository.findAll()
                .stream()
                // 이름 또는 이메일에 키워드가 포함된 회원 필터링
                .filter(m -> m.getName().contains(keyword)
                        || m.getEmail().contains(keyword))
                .collect(Collectors.toList());
    }
}