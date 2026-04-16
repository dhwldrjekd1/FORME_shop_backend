package com.forme.shop.review.dto;

import com.forme.shop.review.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 클라이언트에게 리뷰 정보를 응답할 때 사용하는 DTO
@Getter
@Builder
public class ReviewResponseDto {

    private Long id;
    private Long memberId;
    private String memberName;    // 리뷰 작성자 이름
    private Long productId;
    private String productName;   // 리뷰 대상 상품명
    private Long orderId;
    private Integer rating;       // 별점
    private String content;       // 리뷰 내용
    private String reply;         // 관리자 답글
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Review 엔티티를 ReviewResponseDto로 변환하는 정적 메서드
    public static ReviewResponseDto from(Review review) {
        return ReviewResponseDto.builder()
                .id(review.getId())
                .memberId(review.getMember().getId())
                .memberName(review.getMember().getName())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .orderId(review.getOrders() != null ? review.getOrders().getId() : null)
                .rating(review.getRating())
                .content(review.getContent())
                .reply(review.getReply())
                .repliedAt(review.getRepliedAt())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}