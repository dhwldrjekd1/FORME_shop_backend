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

        // productId가 없으면(요청 바디에 productId 키 자체가 빠진 경우) productRepository.
        // existsById(null)이 Spring Data의 Assert.notNull에 걸려 "The given id must not be
        // null!" 같은 영문 프레임워크 내부 메시지를 그대로 던지고, 컨트롤러가 그 메시지를
        // 그대로 클라이언트에 돌려주게 된다 — 다른 API들과 다르게 도메인 메시지가 아닌
        // 프레임워크 내부 문구가 노출되는 걸 막기 위해 여기서 먼저 걸러낸다.
        if (productId == null) {
            throw new IllegalArgumentException("상품을 선택해주세요.");
        }
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
}
