package com.forme.shop.order.repository;

import com.forme.shop.order.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Orders, Long> {

    // SELECT * FROM orders WHERE member_id = ? ORDER BY created_at DESC
    // 특정 회원의 주문 목록 최신순 조회
    List<Orders> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    // SELECT * FROM orders ORDER BY created_at DESC
    // 관리자 - 전체 주문 목록 최신순 조회
    List<Orders> findAllByOrderByCreatedAtDesc();

    // SELECT * FROM orders WHERE status = ?
    // 관리자 - 상태별 주문 목록 조회 (배송관리 등)
    List<Orders> findByStatus(String status);
}