package com.bluebottle.shop.cart.repository;

import com.bluebottle.shop.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// JpaRepository 상속으로 기본 CRUD 자동 제공
// <Cart, Long> = <엔티티 타입, PK 타입>
public interface CartRepository extends JpaRepository<Cart, Long> {

    // SELECT * FROM cart WHERE member_id = ?
    // 특정 회원의 장바구니 전체 조회
    List<Cart> findByMemberId(Long memberId);

    // SELECT * FROM cart WHERE member_id = ? AND product_id = ?
    // 특정 회원의 특정 상품 장바구니 조회
    // 이미 담긴 상품인지 확인할 때 사용
    Optional<Cart> findByMemberIdAndProductId(Long memberId, Long productId);

    // DELETE FROM cart WHERE member_id = ?
    // 특정 회원의 장바구니 전체 삭제
    // 주문 완료 후 장바구니 비울 때 사용
    void deleteByMemberId(Long memberId);
}