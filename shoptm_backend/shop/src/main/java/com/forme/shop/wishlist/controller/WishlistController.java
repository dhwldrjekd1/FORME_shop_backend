package com.forme.shop.wishlist.controller;

import com.forme.shop.wishlist.dto.WishlistResponseDto;
import com.forme.shop.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    // 찜 목록 조회
    @GetMapping("/members/{memberId}/wishlist")
    public ResponseEntity<List<WishlistResponseDto>> getWishlist(@PathVariable Long memberId) {
        return ResponseEntity.ok(wishlistService.getWishlist(memberId));
    }

    // 찜 추가
    @PostMapping("/members/{memberId}/wishlist")
    public ResponseEntity<?> addWishlist(@PathVariable Long memberId, @RequestBody Map<String, Long> body) {
        try {
            return ResponseEntity.ok(wishlistService.addWishlist(memberId, body.get("productId")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 찜 삭제
    @DeleteMapping("/members/{memberId}/wishlist/{productId}")
    public ResponseEntity<Void> removeWishlist(@PathVariable Long memberId, @PathVariable Long productId) {
        wishlistService.removeWishlist(memberId, productId);
        return ResponseEntity.noContent().build();
    }
}
