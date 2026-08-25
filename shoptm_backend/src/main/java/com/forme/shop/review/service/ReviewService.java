package com.forme.shop.review.service;

import com.forme.shop.common.security.SecurityUtil;
import com.forme.shop.member.service.MemberService;
import com.forme.shop.order.entity.Orders;
import com.forme.shop.order.repository.OrderRepository;
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
    private final MemberService memberService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    // 특정 상품의 리뷰 목록 조회 (일반회원)
    public List<ReviewResponseDto> getProductReviews(Long productId) {
        return reviewRepository.findByProductIdAndIsActiveTrueOrderByCreatedAtDesc(productId)
                .stream()
                .map(ReviewResponseDto::from)
                .collect(Collectors.toList());
    }

    // 내가 작성한 리뷰 목록 조회 (일반회원) — 작성/수정/삭제와 달리 이 조회에는 소유자 검증이
    // 통째로 빠져 있어서, memberId만 바꿔서 다른 회원이 쓴 리뷰 목록을 그대로 볼 수 있었음
    public List<ReviewResponseDto> getMyReviews(Long memberId) {
        memberService.findSelfOrAdminMember(memberId);

        return reviewRepository.findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(memberId)
                .stream()
                .map(ReviewResponseDto::from)
                .collect(Collectors.toList());
    }

    // 리뷰 작성 (일반회원)
    // 구매 인증(orderId)은 선택 — 있으면 본인 주문인지 검증 후 연결, 없어도 작성 가능.
    // 다만 구매 인증 여부와 무관하게 회원당 상품 하나에 리뷰 하나만 작성 가능(중복 방지)
    @Transactional
    public ReviewResponseDto createReview(Long memberId, ReviewRequestDto.Create dto) {

        // 본인(또는 관리자) 명의로만 리뷰 작성 가능
        memberService.findSelfOrAdminMember(memberId);

        if (!productRepository.existsById(dto.getProductId())) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }

        // 주문 확인 (선택 — orderId가 null이면 "구매 인증" 배지 없이 리뷰 가능)
        if (dto.getOrderId() != null) {
            Orders orders = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

            // 구매확인(orders 연결)은 실제로 본인이 그 상품을 주문했을 때만 성립해야 하므로,
            // 남의 주문이거나 그 주문에 없는 상품을 걸어 "구매 인증" 리뷰를 위조하지 못하도록 검증
            if (!orders.getMember().getId().equals(memberId)) {
                throw new IllegalArgumentException("본인의 주문에 대해서만 리뷰를 작성할 수 있습니다.");
            }
            boolean containsProduct = orders.getOrderItems().stream()
                    .anyMatch(item -> item.getProduct().getId().equals(dto.getProductId()));
            if (!containsProduct) {
                throw new IllegalArgumentException("해당 주문에 포함되지 않은 상품입니다.");
            }
        }

        // 이미 리뷰가 있으면 삽입하지 않는 원자적 삽입(DB UNIQUE(member_id, product_id) 제약 기반).
        // "확인 후 insert" 방식은 두 요청이 동시에 들어오면(연속 클릭 등) 둘 다 "아직 없음"으로 보고
        // 둘 다 insert를 시도해 DB 제약 위반으로 500이 나거나(장바구니/찜과 같은 이유), orderId가
        // 없는 리뷰끼리는 예전 제약(member_id, order_id, product_id)로도 NULL이 걸러지지 않아
        // 무제한 작성이 가능했음 — 원자적 삽입 하나로 두 문제를 함께 막는다.
        int inserted = reviewRepository.insertReviewIfAbsent(
                memberId, dto.getProductId(), dto.getOrderId(), dto.getRating(), dto.getContent());
        if (inserted == 0) {
            throw new IllegalArgumentException("이미 리뷰를 작성했습니다.");
        }

        return reviewRepository.findByMemberIdAndProductId(memberId, dto.getProductId())
                .map(ReviewResponseDto::from)
                .orElseThrow(() -> new IllegalStateException("리뷰를 찾을 수 없습니다."));
    }

    // 리뷰 수정 (일반회원)
    @Transactional
    public ReviewResponseDto updateReview(Long reviewId, ReviewRequestDto.Update dto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));

        // 본인(또는 관리자) 소유의 리뷰만 수정 가능
        SecurityUtil.checkOwnerOrAdmin(review.getMember().getEmail());

        // null 체크 후 값이 있을 때만 수정 (부분 수정 가능)
        if (dto.getRating()  != null) review.setRating(dto.getRating());
        if (dto.getContent() != null) review.setContent(dto.getContent());

        // @Transactional 덕분에 save() 없이도 변경사항 자동 반영 (더티 체킹)
        return ReviewResponseDto.from(review);
    }

    // 리뷰 삭제 (일반회원 또는 관리자) — hard delete (UNIQUE 제약 때문)
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));

        // 본인(또는 관리자) 소유의 리뷰만 삭제 가능
        SecurityUtil.checkOwnerOrAdmin(review.getMember().getEmail());

        reviewRepository.delete(review);
    }

    // 관리자 답글
    @Transactional
    public ReviewResponseDto replyReview(Long reviewId, String reply) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));
        review.setReply(reply);
        review.setRepliedAt(reply != null ? java.time.LocalDateTime.now() : null);
        return ReviewResponseDto.from(review);
    }

    // 관리자 - 전체 리뷰 목록 조회
    public List<ReviewResponseDto> getAllReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ReviewResponseDto::from)
                .collect(Collectors.toList());
    }
}