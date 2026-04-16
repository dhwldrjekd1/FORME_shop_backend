package com.forme.shop.review.controller;

import com.forme.shop.review.dto.ReviewRequestDto;
import com.forme.shop.review.dto.ReviewResponseDto;
import com.forme.shop.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController               // @Controller + @ResponseBody: JSON 형태로 응답 반환
@RequestMapping("/api")       // 이 컨트롤러의 모든 URL 앞에 /api 붙음
@RequiredArgsConstructor      // Lombok: final 필드 생성자 주입 자동 처리
public class ReviewController {

    private final ReviewService reviewService;

    // 특정 상품의 리뷰 목록 조회 (일반회원)
    // GET /api/products/{productId}/reviews
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<List<ReviewResponseDto>> getProductReviews(
            @PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId));
    }

    // 내가 작성한 리뷰 목록 조회 (일반회원)
    // GET /api/members/{memberId}/reviews
    @GetMapping("/members/{memberId}/reviews")
    public ResponseEntity<List<ReviewResponseDto>> getMyReviews(
            @PathVariable Long memberId) {
        return ResponseEntity.ok(reviewService.getMyReviews(memberId));
    }

    // 리뷰 작성 (일반회원)
    // POST /api/members/{memberId}/reviews
    @PostMapping("/members/{memberId}/reviews")
    public ResponseEntity<?> createReview(
            @PathVariable Long memberId,
            @Valid @RequestBody ReviewRequestDto.Create dto) {
        try {
            return ResponseEntity.ok(reviewService.createReview(memberId, dto));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of(
                "message", "리뷰 작성 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage()
            ));
        }
    }

    // 리뷰 수정 (일반회원)
    // PUT /api/reviews/{reviewId}
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponseDto> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewRequestDto.Update dto) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, dto));
    }

    // 리뷰 삭제 (일반회원)
    // DELETE /api/reviews/{reviewId}
    // 204 No Content: 처리 성공했지만 반환할 데이터 없음
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    // 관리자 - 전체 리뷰 목록
    // GET /api/admin/reviews
    @GetMapping("/admin/reviews")
    public ResponseEntity<List<ReviewResponseDto>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    // 관리자 - 리뷰 답글
    // POST /api/admin/reviews/{reviewId}/reply
    @PostMapping("/admin/reviews/{reviewId}/reply")
    public ResponseEntity<ReviewResponseDto> replyReview(
            @PathVariable Long reviewId,
            @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(reviewService.replyReview(reviewId, body.get("reply")));
    }

    // 관리자 - 답글 삭제
    // DELETE /api/admin/reviews/{reviewId}/reply
    @DeleteMapping("/admin/reviews/{reviewId}/reply")
    public ResponseEntity<ReviewResponseDto> deleteReply(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.replyReview(reviewId, null));
    }

    // 관리자 - 리뷰 삭제
    // DELETE /api/admin/reviews/{reviewId}
    @DeleteMapping("/admin/reviews/{reviewId}")
    public ResponseEntity<Void> adminDeleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}