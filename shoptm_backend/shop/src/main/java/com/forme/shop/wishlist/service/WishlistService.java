package com.forme.shop.wishlist.service;

import com.forme.shop.member.entity.Member;
import com.forme.shop.member.repository.MemberRepository;
import com.forme.shop.product.entity.Product;
import com.forme.shop.product.repository.ProductRepository;
import com.forme.shop.wishlist.dto.WishlistResponseDto;
import com.forme.shop.wishlist.entity.Wishlist;
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
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    public List<WishlistResponseDto> getWishlist(Long memberId) {
        return wishlistRepository.findByMemberId(memberId).stream()
                .map(WishlistResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public WishlistResponseDto addWishlist(Long memberId, Long productId) {
        if (wishlistRepository.existsByMemberIdAndProductId(memberId, productId)) {
            throw new IllegalArgumentException("이미 찜한 상품입니다.");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        Wishlist wishlist = Wishlist.builder().member(member).product(product).build();
        return WishlistResponseDto.from(wishlistRepository.save(wishlist));
    }

    @Transactional
    public void removeWishlist(Long memberId, Long productId) {
        wishlistRepository.deleteByMemberIdAndProductId(memberId, productId);
    }

    public boolean isWished(Long memberId, Long productId) {
        return wishlistRepository.existsByMemberIdAndProductId(memberId, productId);
    }
}
