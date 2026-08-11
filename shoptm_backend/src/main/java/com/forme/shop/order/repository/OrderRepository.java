package com.forme.shop.order.repository;

import com.forme.shop.order.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Orders, Long> {

    // 회원 본인 취소용: PAID 상태일 때만 원자적으로 CANCELLED로 전이한다. 반환값(영향받은
    // 행 수)이 0이면 그 사이 다른 요청이 먼저 상태를 바꾼 것 — 재고 복구를 딱 한 번만
    // 하도록(같은 주문을 동시에 두 번 취소해도 재고가 두 번 복구되지 않도록) 이 원자성이 필요함.
    // updatedAt은 @UpdateTimestamp가 엔티티 setter 경로에서만 자동 갱신되고 이런 벌크
    // UPDATE는 그 경로를 안 타므로, 취소 시각이 반영되도록 여기서 직접 세팅한다.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Orders o SET o.status = 'CANCELLED', o.updatedAt = CURRENT_TIMESTAMP WHERE o.id = :orderId AND o.status = 'PAID'")
    int cancelIfPaid(@Param("orderId") Long orderId);

    // 관리자 상태변경용: CANCELLED가 아니었던 주문만 원자적으로 CANCELLED로 전이한다.
    // (관리자는 PAID 외의 상태에서도 취소 처리할 수 있어 cancelIfPaid보다 조건이 느슨함)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Orders o SET o.status = 'CANCELLED', o.updatedAt = CURRENT_TIMESTAMP WHERE o.id = :orderId AND o.status <> 'CANCELLED'")
    int cancelIfNotCancelled(@Param("orderId") Long orderId);

    // 관리자 상태변경용(취소 외 상태): CANCELLED가 아닌 주문만 원자적으로 상태를 바꾼다.
    // 이 조건이 없으면, 회원이 막 취소해서 재고까지 복구된 주문을 관리자의 다른 상태변경
    // 요청(예: SHIPPED)이 거의 동시에 덮어써서 "재고는 복구됐는데 주문은 취소 아님"으로
    // 남는 유실이 생길 수 있다(교차검증 중 재현 가능한 경로로 지적받아 반영).
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Orders o SET o.status = :newStatus, o.updatedAt = CURRENT_TIMESTAMP WHERE o.id = :orderId AND o.status <> 'CANCELLED'")
    int updateStatusIfNotCancelled(@Param("orderId") Long orderId, @Param("newStatus") String newStatus);

    // SELECT * FROM orders WHERE member_id = ? ORDER BY created_at DESC
    // 특정 회원의 주문 목록 최신순 조회
    List<Orders> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    // SELECT * FROM orders ORDER BY created_at DESC
    // 관리자 - 전체 주문 목록 최신순 조회
    List<Orders> findAllByOrderByCreatedAtDesc();

    // 관리자 대시보드 전용 - member/orderItems/orderItems.product를 한 번에 JOIN FETCH.
    // AdminService.getDashboard()가 브랜드별 매출을 집계하며 주문마다 orderItems를,
    // 주문상품마다 product를 읽어서 LAZY 관계 그대로면 주문 수만큼 N+1이 발생했음.
    // 컬렉션(orderItems)을 JOIN FETCH하면 행이 중복될 수 있어 DISTINCT로 정리.
    @Query("SELECT DISTINCT o FROM Orders o " +
            "LEFT JOIN FETCH o.member " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.product " +
            "ORDER BY o.createdAt DESC")
    List<Orders> findAllWithDetailsOrderByCreatedAtDesc();

    // SELECT * FROM orders WHERE status = ?
    // 관리자 - 상태별 주문 목록 조회 (배송관리 등)
    List<Orders> findByStatus(String status);
}