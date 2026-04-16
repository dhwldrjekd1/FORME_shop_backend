package com.forme.shop.cart.controller;

import com.forme.shop.cart.dto.CartRequestDto;
import com.forme.shop.cart.dto.CartResponseDto;
import com.forme.shop.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController               // @Controller + @ResponseBody: JSON 형태로 응답 반환
@RequestMapping("/api")       // 이 컨트롤러의 모든 URL 앞에 /api 붙음
@RequiredArgsConstructor      // Lombok: final 필드 생성자 주입 자동 처리
public class CartController {

    private final CartService cartService;

    // 장바구니 목록 조회
    // GET /api/members/{memberId}/cart
    @GetMapping("/members/{memberId}/cart")
    public ResponseEntity<List<CartResponseDto>> getCartList(
            @PathVariable Long memberId) {  // URL 경로의 {memberId} 값을 파라미터로 받음
        return ResponseEntity.ok(cartService.getCartList(memberId));
    }

    // 장바구니 담기
    // POST /api/members/{memberId}/cart
    @PostMapping("/members/{memberId}/cart")
    public ResponseEntity<CartResponseDto> addCart(
            @PathVariable Long memberId,
            @Valid @RequestBody CartRequestDto.Add dto) {  // @Valid: 유효성 검증 활성화
        return ResponseEntity.ok(cartService.addCart(memberId, dto));
    }

    // 장바구니 수량 수정
    // PATCH /api/cart/{cartId}
    // PATCH: 일부 데이터만 수정할 때 사용
    @PatchMapping("/cart/{cartId}")
    public ResponseEntity<CartResponseDto> updateCart(
            @PathVariable Long cartId,
            @Valid @RequestBody CartRequestDto.Update dto) {
        return ResponseEntity.ok(cartService.updateCart(cartId, dto));
    }

    // 장바구니 단건 삭제
    // DELETE /api/cart/{cartId}
    // 204 No Content: 처리 성공했지만 반환할 데이터 없음
    @DeleteMapping("/cart/{cartId}")
    public ResponseEntity<Void> deleteCart(@PathVariable Long cartId) {
        cartService.deleteCart(cartId);
        return ResponseEntity.noContent().build();
    }

    // 장바구니 전체 삭제
    // DELETE /api/members/{memberId}/cart
    @DeleteMapping("/members/{memberId}/cart")
    public ResponseEntity<Void> deleteAllCart(@PathVariable Long memberId) {
        cartService.deleteAllCart(memberId);
        return ResponseEntity.noContent().build();
    }
}