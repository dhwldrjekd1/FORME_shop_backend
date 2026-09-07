package com.forme.shop.product.repository;

import com.forme.shop.product.entity.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {

    // 이 상품이 사이즈별로 재고를 관리하는 상품인지(=product_sizes 행이 하나라도 있는지) 확인.
    // 없으면(사이즈 구분이 없는 상품) 주문 시 사이즈별 검증 자체를 건너뛰어야 한다.
    boolean existsByProductId(Long productId);

    // 재고 차감이 실패했을 때, "그 사이즈 자체가 없음"과 "그 사이즈는 있는데 재고만 부족함"을
    // 구분해 더 정확한 안내 메시지를 주는 데 사용 (OrderService.createOrder 참고).
    boolean existsByProductIdAndSize(Long productId, String size);

    // ProductRepository.decreaseStockIfAvailable과 동일한 이유로 원자적 UPDATE — 전체 재고만
    // 확인/차감하면, 특정 사이즈가 품절이어도 다른 사이즈에 재고가 남아있는 한(=전체 합계가
    // 0이 아닌 한) 그 품절 사이즈 주문이 그냥 통과해버렸다. 그래서 전체 재고 차감과 별개로
    // 이 사이즈 행도 같은 방식으로 확인/차감해야 한다.
    // 반환값(영향받은 행 수)이 0이면 그 사이즈 재고가 부족하거나(0 &lt; 필요수량), 애초에 그
    // product_id+size 조합의 행이 존재하지 않는 것(예: 프런트가 안 보내주는 사이즈명을 그대로
    // 보낸 경우) — 둘 다 "이 사이즈로는 주문 불가"로 동일하게 처리하면 된다.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductSize ps SET ps.stock = ps.stock - :quantity " +
            "WHERE ps.product.id = :productId AND ps.size = :size AND ps.stock >= :quantity")
    int decreaseStockIfAvailable(@Param("productId") Long productId, @Param("size") String size,
                                  @Param("quantity") Integer quantity);

    // 주문 취소 시 사이즈별 재고 복구. ProductRepository.increaseStock과 달리 영향받은 행이
    // 0이어도(그 사이 관리자가 그 사이즈를 삭제했거나 상품을 사이즈 미관리로 바꾼 경우) 오류로
    // 취급하지 않는다 — 전체 재고는 이미 별도로 정상 복구되므로, 복구할 사이즈 행이 없는 것뿐
    // 주문 취소 자체가 실패할 이유는 아니다(OrderService.restoreStock 참고).
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProductSize ps SET ps.stock = ps.stock + :quantity " +
            "WHERE ps.product.id = :productId AND ps.size = :size")
    int increaseStock(@Param("productId") Long productId, @Param("size") String size,
                       @Param("quantity") Integer quantity);
}
