package com.forme.shop.review.service;

import com.forme.shop.member.entity.Member;
import com.forme.shop.member.repository.MemberRepository;
import com.forme.shop.order.entity.Orders;
import com.forme.shop.order.repository.OrderRepository;
import com.forme.shop.product.entity.Product;
import com.forme.shop.product.repository.ProductRepository;
import com.forme.shop.review.dto.ReviewRequestDto;
import com.forme.shop.review.dto.ReviewResponseDto;
import com.forme.shop.review.entity.Review;
import com.forme.shop.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // 기본적으로 읽기 전용 트랜잭션 (조회 성능 최적화)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    // 특정 상품의 리뷰 목록 조회 (일반회원)
    public List<ReviewResponseDto> getProductReviews(Long productId) {
        return reviewRepository.findByProductIdAndIsActiveTrueOrderByCreatedAtDesc(productId)
                .stream()
                .map(ReviewResponseDto::from)
                .collect(Collectors.toList());
    }

    // 내가 작성한 리뷰 목록 조회 (일반회원)
    public List<ReviewResponseDto> getMyReviews(Long memberId) {
        return reviewRepository.findByProductIdAndIsActiveTrueOrderByCreatedAtDesc(memberId)
                .stream()
                .map(ReviewResponseDto::from)
                .collect(Collectors.toList());
    }

    // 리뷰 작성 (일반회원)
    // 구매한 상품에 대해서만 작성 가능, 중복 작성 불가
    @Transactional
    public ReviewResponseDto createReview(Long memberId, ReviewRequestDto.Create dto) {

        // 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 상품 존재 여부 확인
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 주문 존재 여부 확인
        Orders orders = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 이미 리뷰를 작성했는지 확인 (중복 방지)
        if (reviewRepository.existsByMemberIdAndOrdersIdAndProductId(
                memberId, dto.getOrderId(), dto.getProductId())) {
            throw new IllegalArgumentException("이미 리뷰를 작성했습니다.");
        }

        Review review = Review.builder()
                .member(member)
                .product(product)
                .orders(orders)
                .rating(dto.getRating())
                .content(dto.getContent())
                .build();

        return ReviewResponseDto.from(reviewRepository.save(review));
    }

    // 리뷰 수정 (일반회원)
    @Transactional
    public ReviewResponseDto updateReview(Long reviewId, ReviewRequestDto.Update dto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));

        // null 체크 후 값이 있을 때만 수정 (부분 수정 가능)
        if (dto.getRating()  != null) review.setRating(dto.getRating());
        if (dto.getContent() != null) review.setContent(dto.getContent());

        // @Transactional 덕분에 save() 없이도 변경사항 자동 반영 (더티 체킹)
        return ReviewResponseDto.from(review);
    }

    // 리뷰 삭제 (일반회원 또는 관리자)
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));
        reviewRepository.delete(review);
    }

    // 관리자 - 전체 리뷰 목록 조회
    public List<ReviewResponseDto> getAllReviews() {
        return reviewRepository.findAll()
                .stream()
                .map(ReviewResponseDto::from)
                .collect(Collectors.toList());
    }
}