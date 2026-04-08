package com.bluebottle.shop.cart.service;

import com.bluebottle.shop.cart.dto.CartRequestDto;
import com.bluebottle.shop.cart.dto.CartResponseDto;
import com.bluebottle.shop.cart.entity.Cart;
import com.bluebottle.shop.cart.repository.CartRepository;
import com.bluebottle.shop.member.entity.Member;
import com.bluebottle.shop.member.repository.MemberRepository;
import com.bluebottle.shop.product.entity.Product;
import com.bluebottle.shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service                          // 스프링 빈으로 등록, 비즈니스 로직 담당
@RequiredArgsConstructor          // Lombok: final 필드를 생성자 주입으로 자동 처리
@Transactional(readOnly = true)   // 기본적으로 읽기 전용 트랜잭션 (조회 성능 최적화)
public class CartService {

    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    // 장바구니 목록 조회
    public List<CartResponseDto> getCartList(Long memberId) {
        return cartRepository.findByMemberId(memberId)
                .stream()
                .map(CartResponseDto::from)   // 각 Cart 엔티티를 DTO로 변환
                .collect(Collectors.toList());
    }

    // 장바구니 담기
    // 이미 담긴 상품이면 수량만 추가 (중복 방지)
    @Transactional
    public CartResponseDto addCart(Long memberId, CartRequestDto.Add dto) {
        // 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 상품 존재 여부 확인
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 이미 장바구니에 담긴 상품인지 확인
        Optional<Cart> existingCart =
                cartRepository.findByMemberIdAndProductId(memberId, dto.getProductId());

        if (existingCart.isPresent()) {
            // 이미 있으면 수량만 추가 (더티 체킹으로 자동 저장)
            Cart cart = existingCart.get();
            cart.setQuantity(cart.getQuantity() + dto.getQuantity());
            return CartResponseDto.from(cart);
        }

        // 없으면 새로 장바구니에 추가
        Cart cart = Cart.builder()
                .member(member)
                .product(product)
                .quantity(dto.getQuantity())
                .build();

        return CartResponseDto.from(cartRepository.save(cart));
    }

    // 장바구니 수량 수정
    @Transactional
    public CartResponseDto updateCart(Long cartId, CartRequestDto.Update dto) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장바구니입니다."));

        cart.setQuantity(dto.getQuantity());  // 수량 변경 (더티 체킹으로 자동 저장)
        return CartResponseDto.from(cart);
    }

    // 장바구니 단건 삭제
    @Transactional
    public void deleteCart(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장바구니입니다."));
        cartRepository.delete(cart);
    }

    // 장바구니 전체 삭제
    // 주문 완료 후 장바구니 비울 때 사용
    @Transactional
    public void deleteAllCart(Long memberId) {
        cartRepository.deleteByMemberId(memberId);
    }
}