package com.forme.shop.cart.service;

import com.forme.shop.cart.dto.CartRequestDto;
import com.forme.shop.cart.dto.CartResponseDto;
import com.forme.shop.cart.entity.Cart;
import com.forme.shop.cart.repository.CartRepository;
import com.forme.shop.common.security.SecurityUtil;
import com.forme.shop.member.service.MemberService;
import com.forme.shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service                          // 스프링 빈으로 등록, 비즈니스 로직 담당
@RequiredArgsConstructor          // Lombok: final 필드를 생성자 주입으로 자동 처리
@Transactional(readOnly = true)   // 기본적으로 읽기 전용 트랜잭션 (조회 성능 최적화)
public class CartService {

    private final CartRepository cartRepository;
    private final MemberService memberService;
    private final ProductRepository productRepository;

    // 장바구니 목록 조회
    public List<CartResponseDto> getCartList(Long memberId) {
        // 본인(또는 관리자)의 장바구니만 조회 가능 — 존재 여부 확인과 소유자 확인을 한 번에
        // 처리하는 이유는 MemberService.findSelfOrAdminMember() 주석 참고 (회원 id 열거 방지)
        memberService.findSelfOrAdminMember(memberId);

        return cartRepository.findByMemberId(memberId)
                .stream()
                .map(CartResponseDto::from)   // 각 Cart 엔티티를 DTO로 변환
                .collect(Collectors.toList());
    }

    // 장바구니 담기
    // 이미 담긴 상품+사이즈면 수량만 추가 (중복 방지)
    @Transactional
    public CartResponseDto addCart(Long memberId, CartRequestDto.Add dto) {
        // 본인(또는 관리자)의 장바구니에만 담을 수 있음
        memberService.findSelfOrAdminMember(memberId);

        // 상품 존재 여부 확인
        if (!productRepository.existsById(dto.getProductId())) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }

        // size가 null이면 빈 문자열로 통일한다. DB unique 제약(member_id, product_id, size)에서
        // Postgres는 NULL끼리 서로 다른 값으로 취급해 ON CONFLICT가 매칭되지 않으므로, size가
        // null인 채로 upsert를 두 번 하면 병합되지 않고 매번 새 행이 생기는 문제가 있었음
        // (현재 프론트는 항상 실제 사이즈나 빈 문자열을 보내 이 경로를 안 타지만, API를 직접
        // 호출하는 경우까지 대비해 서버에서 정규화)
        String size = dto.getSize() != null ? dto.getSize() : "";

        // 있으면 수량만 더하고 없으면 새로 담는 원자적 upsert — 동시에 같은 상품을
        // 처음 두 번 담아도(더블클릭 등) DB가 이 전체를 하나의 연산으로 처리해 안전함
        cartRepository.upsertCart(memberId, dto.getProductId(), size, dto.getQuantity());

        return cartRepository.findByMemberIdAndProductIdAndSize(memberId, dto.getProductId(), size)
                .map(CartResponseDto::from)
                .orElseThrow(() -> new IllegalStateException("장바구니 항목을 찾을 수 없습니다."));
    }

    // 장바구니 수량 수정
    @Transactional
    public CartResponseDto updateCart(Long cartId, CartRequestDto.Update dto) {
        Cart cart = findSelfOrAdminCart(cartId);

        cart.setQuantity(dto.getQuantity());  // 수량 변경 (더티 체킹으로 자동 저장)
        return CartResponseDto.from(cart);
    }

    // 장바구니 단건 삭제
    @Transactional
    public void deleteCart(Long cartId) {
        Cart cart = findSelfOrAdminCart(cartId);

        cartRepository.delete(cart);
    }

    // "장바구니 항목 id로 대상을 찾되, 본인 것이거나 관리자일 때만 결과를 내어준다"를 한 번에
    // 처리한다. MemberService.findSelfOrAdminMember()와 같은 이유(존재 여부 열거 방지) —
    // 대상을 먼저 조회해서 없으면 400, 있는데 내 게 아니면 403을 따로 응답하면, 로그인만 한
    // 상태로 남의 cartId를 넣어봤을 때 그 응답 차이만으로 유효한 장바구니 항목 id를 하나씩
    // 찾아낼 수 있었다.
    private Cart findSelfOrAdminCart(Long cartId) {
        Cart cart = cartRepository.findById(cartId).orElse(null);
        if (SecurityUtil.isAdmin()) {
            if (cart == null) {
                throw new IllegalArgumentException("존재하지 않는 장바구니입니다.");
            }
            return cart;
        }
        if (cart == null || !SecurityUtil.isSelf(cart.getMember().getEmail())) {
            throw new AccessDeniedException("본인의 정보만 접근할 수 있습니다.");
        }
        return cart;
    }

    // 장바구니 전체 삭제
    // 주문 완료 후 장바구니 비울 때 사용
    @Transactional
    public void deleteAllCart(Long memberId) {
        // 본인(또는 관리자)의 장바구니만 비울 수 있음
        memberService.findSelfOrAdminMember(memberId);

        cartRepository.deleteByMemberId(memberId);
    }
}