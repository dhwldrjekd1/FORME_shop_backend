package com.forme.shop.order.repository;

import com.forme.shop.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // SELECT * FROM order_item WHERE order_id = ?
    // 특정 주문의 상세 목록 조회
    List<OrderItem> findByOrdersId(Long orderId);
}