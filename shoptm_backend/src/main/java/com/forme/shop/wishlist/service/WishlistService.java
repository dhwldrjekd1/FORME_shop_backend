package com.forme.shop.wishlist.service;

import com.forme.shop.member.service.MemberService;
import com.forme.shop.product.repository.ProductRepository;
import com.forme.shop.wishlist.dto.WishlistResponseDto;
import com.forme.shop.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final MemberService memberService;
    private final ProductRepository productRepository;

    public List<WishlistResponseDto> getWishlist(Long memberId) {
        // 본인(또는 관리자)의 찜 목록만 조회 가능 — 존재 여부 확인과 소유자 확인을 한 번에
        // 처리하는 이유는 MemberService.findSelfOrAdminMember() 주석 참고 (회원 id 열거 방지)
        memberService.findSelfOrAdminMember(memberId);

        return wishlistRepository.findByMemberId(memberId).stream()
                .map(WishlistResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public WishlistResponseDto addWishlist(Long memberId, Long productId) {
        // 본인(또는 관리자)의 찜 목록에만 추가 가능
        memberService.findSelfOrAdminMember(memberId);

        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }

        // 있으면 조용히 무시하고 없으면 새로 담는 원자적 upsert — 동시에 같은 상품을
        // 두 번 찜해도(하트 연타 등) DB가 하나의 연산으로 처리해 안전함
        wishlistRepository.upsertWishlist(memberId, productId);

        return wishlistRepository.findByMemberIdAndProductId(memberId, productId)
                .map(WishlistResponseDto::from)
                .orElseThrow(() -> new IllegalStateException("찜 항목을 찾을 수 없습니다."));
    }

    @Transactional
    public void removeWishlist(Long memberId, Long productId) {
        // 본인(또는 관리자) 소유의 찜 목록에서만 삭제 가능
        memberService.findSelfOrAdminMember(memberId);

        wishlistRepository.deleteByMemberIdAndProductId(memberId, productId);
    }

    public boolean isWished(Long memberId, Long productId) {
        return wishlistRepository.existsByMemberIdAndProductId(memberId, productId);
    }
}
