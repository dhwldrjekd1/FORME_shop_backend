package com.bluebottle.shop.delivery.repository;

import com.bluebottle.shop.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // SELECT * FROM deliveries WHERE order_id = ?
    // 특정 주문의 배송 정보 조회
    Optional<Delivery> findByOrdersId(Long orderId);

    // SELECT * FROM deliveries WHERE status = ?
    // 관리자 - 배송 상태별 목록 조회 (ex: IN_TRANSIT 인 것만)
    List<Delivery> findByStatus(String status);
}