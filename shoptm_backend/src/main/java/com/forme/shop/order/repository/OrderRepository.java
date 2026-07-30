package com.forme.shop.order.repository;

import com.forme.shop.order.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<Orders, Long> {

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