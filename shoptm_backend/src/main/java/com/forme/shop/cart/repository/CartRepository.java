package com.forme.shop.cart.repository;

import com.forme.shop.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// JpaRepository 상속으로 기본 CRUD 자동 제공
// <Cart, Long> = <엔티티 타입, PK 타입>
public interface CartRepository extends JpaRepository<Cart, Long> {

    // SELECT * FROM cart WHERE member_id = ?
    // 특정 회원의 장바구니 전체 조회
    List<Cart> findByMemberId(Long memberId);

    // SELECT * FROM cart WHERE member_id = ? AND product_id = ? AND size = ?
    // 특정 회원의 특정 상품+사이즈 장바구니 조회 (이미 담긴 항목인지 확인할 때 사용)
    // 파생 쿼리라 size가 null이어도 Spring Data가 자동으로 "size IS NULL"로 비교해줌
    Optional<Cart> findByMemberIdAndProductIdAndSize(Long memberId, Long productId, String size);

    // 있으면 수량만 더하고 없으면 새로 담는 원자적 upsert. "조회 후 있으면 UPDATE, 없으면
    // INSERT" 방식은 동시에 같은 상품을 처음 담을 때(더블클릭 등) 두 요청이 똑같이 "없음"을
    // 보고 둘 다 INSERT를 시도해 DB unique 제약 위반(500)이 날 수 있고, 그 예외를 잡아서
    // 같은 트랜잭션에서 재시도하는 방식도 시도해봤지만 실패한 INSERT 이후 Hibernate 세션
    // 자체가 오염돼(HHH000099 어설션 실패) 재시도조차 실패하는 걸 직접 겪었음. INSERT ...
    // ON CONFLICT DO UPDATE는 DB가 이 전체를 하나의 원자적 연산으로 처리해줘서 애초에
    // 이런 경쟁/세션 오염이 성립하지 않는다.
    // DO UPDATE의 더한 결과를 LEAST(..., 999)로 묶어둔다 — CartRequestDto.Add.quantity의
    // @Max(999)는 "이번 한 번의 요청"만 999 이하로 막을 뿐, 누적된 기존 수량과 합친 결과까지는
    // 막지 않는다. 그래서 같은 상품을 여러 번(예: 999개씩 두 번) 담으면 장바구니엔 999를 넘는
    // 수량이 그대로 쌓일 수 있었다(다른 방향에서 버그를 찾다가 발견) — 수량 조절기는 절대값
    // 1~999만 설정 가능해 그렇게 쌓인 수량을 줄일 수도, 결제도 안 되는(OrderItemDto.quantity도
    // 같은 999 상한) 막다른 상태가 됐다. LEAST로 합친 결과 자체를 상한에 가둬 애초에 그 상태가
    // 생기지 않게 한다.
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO cart_items (member_id, product_id, size, quantity, created_at, updated_at) " +
            "VALUES (:memberId, :productId, :size, :quantity, now(), now()) " +
            "ON CONFLICT (member_id, product_id, size) " +
            "DO UPDATE SET quantity = LEAST(cart_items.quantity + :quantity, 999), updated_at = now()",
            nativeQuery = true)
    void upsertCart(@Param("memberId") Long memberId, @Param("productId") Long productId,
                     @Param("size") String size, @Param("quantity") Integer quantity);

    // DELETE FROM cart WHERE member_id = ?
    // 특정 회원의 장바구니 전체 삭제
    // 주문 완료 후 장바구니 비울 때 사용
    void deleteByMemberId(Long memberId);
}