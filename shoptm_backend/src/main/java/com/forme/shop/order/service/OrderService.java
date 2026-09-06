package com.forme.shop.order.service;

import com.forme.shop.common.security.SecurityUtil;
import com.forme.shop.member.entity.Member;
import com.forme.shop.member.repository.MemberRepository;
import com.forme.shop.member.service.MemberService;
import com.forme.shop.order.dto.OrderRequestDto;
import com.forme.shop.order.dto.OrderResponseDto;
import com.forme.shop.order.entity.OrderItem;
import com.forme.shop.order.entity.Orders;
import com.forme.shop.order.repository.OrderRepository;
import com.forme.shop.payment.entity.Payment;
import com.forme.shop.payment.service.PaymentService;
import com.forme.shop.product.entity.Product;
import com.forme.shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;

    // 주문 생성 (일반회원)
    @Transactional
    public OrderResponseDto createOrder(Long memberId, OrderRequestDto.Create dto) {

        // 본인(또는 관리자)만 자신의 이름으로 주문 생성 가능
        Member member = memberService.findSelfOrAdminMember(memberId);

        // 토스 결제를 거쳐 들어온 주문이면, 이 결제를 "지금 이 요청이" 쓰겠다고 먼저 선점한다.
        // 같은 paymentKey로 거의 동시에 두 번째 요청이 들어오면(더블클릭, 느린 응답 후 재시도)
        // 선점에 실패하므로, 하나의 결제로 주문이 두 번 만들어지는 것을 여기서 원천 차단한다.
        // 빈 문자열도 "결제 없음"과 동일하게 취급 (null과 구분해서 다르게 처리할 이유가 없음)
        String paymentKey = (dto.getPaymentKey() != null && !dto.getPaymentKey().isBlank())
                ? dto.getPaymentKey() : null;
        Payment claimedPayment = null;
        if (paymentKey != null) {
            // 선점(claim)을 시도하기 전에 이 결제를 실제로 승인받은 사람과 지금 주문을 만들
            // 대상 회원이 같은지부터 확인한다. paymentKey 값만 알아내면(성공 리다이렉트 URL
            // 쿼리 파라미터로 남는 값이라 브라우저 히스토리·Referer 등으로 노출될 수 있음)
            // 다른 계정으로 로그인해 남이 이미 결제한 건을 자기 주문으로 가로챌 수 있었음.
            // 선점부터 하고 소유자가 다르면 되돌리는 순서로 짜면, 그 사이의 아주 짧은 순간이라도
            // 진짜 소유자의 동시 요청이 "이미 처리 중"으로 잘못 거부될 수 있는 경쟁 상태가
            // 생기므로, 아예 선점 이전에 확인해 그 경쟁 자체를 없앤다.
            // memberEmail이 null이면(이 컬럼이 생기기 전에 이미 CONFIRMED로 남아있던 결제 —
            // 주문 생성 전 이탈 등으로 orphan된 기존 행) 소유자를 아예 모르는 상태이므로 이
            // 확인을 건너뛴다. null을 "내 게 아님"으로 취급해 막아버리면, 이 컬럼 도입 시점에
            // 우연히 남아있던 정상 결제의 진짜 소유자가 영구히 주문을 완료하지 못하고(자동 환불
            // 경로도 타지 않아 수동 확인 없이는 복구 불가) 발이 묶인다. 이 컬럼 도입 이후 생성되는
            // 결제는 TossController에서 항상 memberEmail을 채우므로 이 우회는 과거 데이터에만 적용된다.
            Payment existingPayment = paymentService.findByPaymentKeyOrNull(paymentKey);
            if (existingPayment != null && existingPayment.getMemberEmail() != null
                    && !member.getEmail().equals(existingPayment.getMemberEmail())) {
                throw new IllegalArgumentException("본인이 진행한 결제로만 주문을 생성할 수 있습니다.");
            }

            if (paymentService.claimForOrderCreation(paymentKey)) {
                claimedPayment = paymentService.getByPaymentKey(paymentKey);
            } else {
                // 선점 실패 — 이미 이 결제로 주문이 만들어져 있다면(재시도) 새로 만들지 않고
                // 원래 만들어진 주문을 그대로 돌려준다(진짜 멱등성). 아직 처리 중이거나
                // 이미 취소된 결제라면 명확한 안내와 함께 거부한다.
                Optional<Orders> existing = paymentService.findLinkedOrder(paymentKey);
                if (existing.isPresent() && existing.get().getMember().getId().equals(memberId)) {
                    return OrderResponseDto.from(existing.get());
                }
                throw new IllegalArgumentException("이미 처리 중이거나 완료·취소된 결제입니다. 잠시 후 다시 확인해주세요.");
            }
        }
        boolean paymentConfirmed = claimedPayment != null;
        // 결제 금액 검증은 클라이언트가 요청 본문에 실어 보낸 paidAmount가 아니라,
        // 결제 승인 시점에 서버가 이미 저장해 둔 진짜 승인 금액(Payment.amount)을 기준으로 한다.
        // (그렇지 않으면 실제로는 소액만 결제한 뒤 paidAmount만 크게 조작해 보내는 위변조가 가능함)
        // paymentKey가 없어(=결제를 거치지 않아) claimedPayment가 없는 경우 dto.getPaidAmount()를
        // 그대로 쓰면, 결제를 전혀 하지 않고도 paidAmount만 totalPrice와 맞춰 보내는 것만으로
        // 바로 PAID 처리가 가능했다(프런트는 이 조합을 쓰지 않지만 API를 직접 호출하면 가능했음).
        // 그래서 결제가 확인되지 않은 경우 클라이언트 값은 아예 무시하고 null로 취급 —
        // 이 경우 주문은 항상 PENDING(결제 없는 데모 주문)으로만 생성된다.
        Integer paidAmount = paymentConfirmed ? claimedPayment.getAmount() : null;

        Orders savedOrders;
        try {
            // 주문 엔티티 생성 (총액은 아래에서 계산 후 설정)
            Orders orders = Orders.builder()
                    .member(member)
                    .receiverName(dto.getReceiverName())
                    .receiverPhone(dto.getReceiverPhone())
                    .address(dto.getAddress())
                    .totalPrice(0)  //  BigDecimal.ZERO → 0 으로 변경
                    .build();

            // 주문 상품 목록 처리
            int totalPrice = 0;  //  BigDecimal → int 로 변경

            for (OrderRequestDto.OrderItemDto itemDto : dto.getItems()) {
                // 상품 존재 여부 확인
                Product product = productRepository.findById(itemDto.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

                // 재고 확인 + 차감을 원자적 UPDATE 한 번으로 처리 (동시 주문으로 인한 오버셀 방지)
                int updated = productRepository.decreaseStockIfAvailable(product.getId(), itemDto.getQuantity());
                if (updated == 0) {
                    throw new IllegalArgumentException(product.getName() + "의 재고가 부족합니다.");
                }

                // 세일 할인 적용된 단가 계산
                int unitPrice = product.getPrice();
                if (product.getDiscountRate() != null && product.getDiscountRate() > 0) {
                    unitPrice = (int) Math.round(product.getPrice() * (1 - product.getDiscountRate() / 100.0));
                }

                // 등급 할인 적용
                int gradeDiscount = getGradeDiscount(member.getGrade());
                if (gradeDiscount > 0) {
                    unitPrice = (int) Math.round(unitPrice * (1 - gradeDiscount / 100.0));
                }

                OrderItem orderItem = OrderItem.builder()
                        .orders(orders)
                        .product(product)
                        .quantity(itemDto.getQuantity())
                        .unitPrice(unitPrice)
                        .size(itemDto.getSize())
                        .build();

                orders.getOrderItems().add(orderItem);

                totalPrice += unitPrice * itemDto.getQuantity();
            }

            orders.setTotalPrice(totalPrice);

            // 실제 결제 승인 금액(paidAmount)이 서버가 방금 계산한 진짜 주문 금액(totalPrice)과
            // 정확히 같은지 검증한다. (금액 위변조 방지 — 위에서 이미 클라이언트 값이 아니라
            // 서버에 저장된 Payment.amount로 대체됐으므로, 여기서는 그 값만 신뢰한다)
            if (paidAmount != null) {
                if (!paidAmount.equals(totalPrice)) {
                    throw new IllegalArgumentException(
                            "결제 금액과 주문 금액이 일치하지 않습니다. (결제: " + paidAmount + "원, 주문: " + totalPrice + "원)");
                }
                orders.setStatus("PAID");
                orders.setPaidAt(LocalDateTime.now());
            }

            // saveAndFlush로 즉시 INSERT를 실행해, DB 제약 위반 등의 실패가 트랜잭션 커밋
            // 시점까지 지연되지 않고 바로 이 try 블록 안에서(환불 로직이 잡을 수 있게) 드러나게 함
            savedOrders = orderRepository.saveAndFlush(orders);
        } catch (RuntimeException e) {
            // 이미 결제가 승인된 뒤였다면(재고 부족 등 어떤 이유로든) 주문을 만들지 못한 채
            // 카드만 결제된 상태로 남지 않도록 여기서 즉시 자동 환불한다.
            if (paymentConfirmed) {
                if (!(e instanceof IllegalArgumentException)) {
                    log.error("주문 생성 중 예기치 못한 오류 (paymentKey={})", paymentKey, e);
                }
                String detail = (e instanceof IllegalArgumentException) ? e.getMessage() : "일시적인 오류가 발생했습니다.";
                // refundAndMarkFailed 자체가 (아주 드물게, 예를 들어 그 트랜잭션의 커밋이 실패하는 등)
                // 또 예외를 던지더라도, 사용자에게는 최소한 "환불도 실패했으니 문의하라"는 안전한
                // 메시지가 반드시 나가야 한다 — 이 지점에서 원본 예외가 그대로 새어나가면 안 됨.
                boolean refunded;
                try {
                    refunded = paymentService.refundAndMarkFailed(paymentKey, e.getMessage());
                } catch (Exception refundError) {
                    log.error("환불 처리 자체가 실패함 (paymentKey={})", paymentKey, refundError);
                    refunded = false;
                }
                throw new IllegalArgumentException(refunded
                        ? "주문 처리 중 오류가 발생해 결제가 자동으로 취소되었습니다. (" + detail + ")"
                        : "주문 처리 중 오류가 발생했고 결제 자동 취소에도 실패했습니다. 고객센터로 문의해주세요. (" + detail + ")");
            }
            throw e;
        }

        OrderResponseDto result = OrderResponseDto.from(savedOrders);

        // 결제 연결/등급 재계산은 부가 처리일 뿐이므로, 여기서 실패하더라도 이미 정상 저장된
        // 주문 자체를 롤백시키거나(전체 트랜잭션 실패) 환불을 잘못 트리거해서는 안 된다.
        if (paymentConfirmed) {
            try {
                paymentService.markLinked(paymentKey, savedOrders);
            } catch (Exception e) {
                log.error("결제-주문 연결 실패 (paymentKey={}, orderId={}) — 주문 자체는 정상 생성됨",
                        paymentKey, savedOrders.getId(), e);
            }
        }

        try {
            // 누적 구매 금액 기반 등급 자동 업그레이드
            updateMemberGrade(member);
        } catch (Exception e) {
            log.error("등급 자동 업그레이드 실패 (memberId={}, orderId={}) — 주문 자체는 정상 생성됨",
                    member.getId(), savedOrders.getId(), e);
        }

        return result;
    }

    // 등급별 할인율
    private int getGradeDiscount(String grade) {
        if (grade == null) return 0;
        return switch (grade.toUpperCase()) {
            case "SILVER" -> 5;
            case "GOLD" -> 8;
            case "VIP" -> 12;
            default -> 0;
        };
    }

    // 누적 구매 금액 계산 → 등급 자동 변경
    private void updateMemberGrade(Member member) {
        // 실제로 결제됐고, 그 뒤 취소/환불도 안 된 주문 금액만 합산한다.
        // - paidAt이 있어야 함: PENDING(결제 없는 데모 주문)은 애초에 결제 자체가 없으므로
        //   status만으로 걸러내면(예전엔 CANCELLED만 제외) 그 금액까지 누적 구매액에 들어가,
        //   실제로 한 푼도 안 낸 회원이 등급 할인 대상(SILVER/GOLD/VIP)이 될 수 있었다.
        // - status가 CANCELLED가 아니어야 함: 취소(cancelIfPaid/cancelIfNotCancelled)는
        //   status만 바꿀 뿐 paidAt은 그대로 남겨두므로, paidAt 조건만으로는 이미 전액 환불된
        //   주문(결제는 했지만 나중에 취소된 것)까지 계속 구매액으로 잡혀버린다 — 두 조건을
        //   모두 만족해야 "실제로 순수하게 결제된 채로 남아있는" 주문이다.
        int totalSpent = orderRepository.findByMemberIdOrderByCreatedAtDesc(member.getId())
                .stream()
                .filter(o -> o.getPaidAt() != null && !"CANCELLED".equals(o.getStatus()))
                .mapToInt(Orders::getTotalPrice)
                .sum();

        String newGrade;
        if (totalSpent >= 1500000) {
            newGrade = "VIP";        // 150만원 이상
        } else if (totalSpent >= 1000000) {
            newGrade = "GOLD";       // 100만원 이상
        } else if (totalSpent >= 500000) {
            newGrade = "SILVER";     // 50만원 이상
        } else {
            newGrade = "BRONZE";     // 50만원 미만
        }

        if (!newGrade.equals(member.getGrade())) {
            member.setGrade(newGrade);
            // 이 시점의 member는 위 decreaseStockIfAvailable()의 clearAutomatically=true로
            // 영속성 컨텍스트가 비워지면서 이미 detach된 상태라, 더티 체킹으로는 저장되지 않는다.
            // (그래서 명시적으로 save 해야 함 - detached 엔티티도 id가 있으면 merge로 정상 반영됨)
            memberRepository.save(member);
        }
    }

    // 내 주문 목록 조회 (일반회원)
    public List<OrderResponseDto> getMyOrders(Long memberId) {
        // 본인(또는 관리자)의 주문 목록만 조회 가능
        memberService.findSelfOrAdminMember(memberId);

        return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(OrderResponseDto::from)
                .collect(Collectors.toList());
    }

    // 주문 단건 조회
    public OrderResponseDto getOrder(Long orderId) {
        Orders orders = findSelfOrAdminOrder(orderId);
        return OrderResponseDto.from(orders);
    }

    // "주문 id로 대상을 찾되, 본인 것이거나 관리자일 때만 결과를 내어준다"를 한 번에 처리한다.
    // MemberService.findSelfOrAdminMember()와 같은 이유(존재 여부 열거 방지) — 대상을 먼저
    // 조회해서 없으면 400, 있는데 내 게 아니면 403을 따로 응답하면, 로그인만 한 상태로 남의
    // 주문 id를 넣어봤을 때 그 응답 차이만으로 유효한 주문 id를 하나씩 찾아낼 수 있었다.
    // DeliveryService도 같은 이유로 이 메서드를 그대로 재사용한다.
    public Orders findSelfOrAdminOrder(Long orderId) {
        Orders orders = orderRepository.findById(orderId).orElse(null);
        if (SecurityUtil.isAdmin()) {
            if (orders == null) {
                throw new IllegalArgumentException("존재하지 않는 주문입니다.");
            }
            return orders;
        }
        if (orders == null || !SecurityUtil.isSelf(orders.getMember().getEmail())) {
            throw new AccessDeniedException("본인의 정보만 접근할 수 있습니다.");
        }
        return orders;
    }

    // 주문 취소 (일반회원)
    // PAID 상태일 때만 취소 가능
    @Transactional
    public void cancelOrder(Long orderId) {
        Orders orders = findSelfOrAdminOrder(orderId);

        // PAID 상태가 아니면 취소 불가 (화면에 보여줄 안내용 사전 확인 — 실제로 "딱 한 번만
        // 취소되게" 보장하는 지점은 아래 cancelIfPaid의 WHERE절. 이 확인만 믿으면, 같은
        // 주문에 대한 취소 요청이 동시에 두 번 들어왔을 때 둘 다 통과해 재고를 두 번
        // 복구할 수 있음)
        if (!"PAID".equals(orders.getStatus())) {
            throw new IllegalArgumentException("취소할 수 없는 주문입니다.");
        }

        // status가 "PAID"였다고 해서 실제로 결제된 게 보장되진 않는다 — 관리자가
        // updateOrderStatus로 결제 없이도 상태만 "PAID"로 바꿔놓을 수 있고, 그 경로는 paidAt을
        // 세팅하지 않는다. 그래서 status 문자열이 아니라 paidAt이 실제로 찍혀있는지로 판단한다.
        // 아래 cancelIfPaid(벌크 UPDATE)가 영속성 컨텍스트를 비우기 전에, 지금 이미 로딩된
        // orders의 스칼라 값을 미리 읽어둔다(updateOrderStatus의 wasPaid와 동일한 이유).
        boolean wasPaid = orders.getPaidAt() != null;

        List<OrderItem> items = new ArrayList<>(orders.getOrderItems());

        int updated = orderRepository.cancelIfPaid(orderId);
        if (updated == 0) {
            // 그 사이 다른 요청이 먼저 취소를 처리한 것 — 중복 복구하지 않고 그대로 종료
            throw new IllegalArgumentException("취소할 수 없는 주문입니다.");
        }

        restoreStock(orderId, items);
        refundIfPaidByToss(orderId, wasPaid, "회원 요청에 의한 주문 취소");
    }

    // 실제 토스 결제로 대금을 받은 주문이었다면 취소 시 그 결제를 환불한다. 재고 복구까지 이미
    // 확정된 뒤에 호출해야 하며, 환불 API 호출 자체가 실패하더라도(네트워크 오류 등) 이미 정상
    // 처리된 주문 취소를 되돌리지 않는다 — PaymentService 쪽에 REFUND_FAILED로 남아 수동 확인
    // 대상이 될 뿐, 여기서 예외를 던져 취소 자체를 실패로 만들면 안 된다.
    // wasPaid: 취소되기 직전 이 주문에 실제로 결제(paidAt)가 있었는지 — PENDING(결제 없는 데모
    // 주문)이 취소되는 경우엔 애초에 연결된 결제가 없는 게 정상이라 구분이 필요함(PaymentService
    // 참고).
    // 실제 토스 환불 API 호출은 되돌릴 수 없는데, 이 메서드가 불리는 시점은 아직 주문 취소
    // 자체를 담은 바깥 트랜잭션이 커밋되기 전이다. 여기서 바로 실행하면 바깥 트랜잭션이 이후
    // 어떤 이유로든 커밋에 실패해 롤백될 경우 "주문은 그대로 PAID인데 환불은 이미 나간" 상태가
    // 생길 수 있어, 실제 호출은 바깥 트랜잭션이 커밋된 뒤(afterCommit)에만 하도록 등록해둔다.
    // 그 afterCommit 콜백은 순전히 메모리 안에서만 예약돼있는 것이라, 바깥 트랜잭션이 커밋된
    // 바로 그 직후 ~ 콜백이 실제로 실행되기 전 사이에 프로세스가 죽으면(배포 재시작, OOM 등)
    // 이 콜백 자체가 통째로 사라져 "환불해야 하는데 그 사실 자체가 어디에도 안 남는" 상황이
    // 생길 수 있다. 그래서 실제 환불 시도 전에, 주문 취소를 커밋하는 바로 그 트랜잭션 안에서
    // 먼저 연결된 결제를 "환불 대기중"으로 표시해 커밋해둔다 — 최악의 경우(콜백 자체가 못
    // 돌아도) 이 상태가 DB에 남아있어 나중에라도 찾아 수동으로 처리할 수 있다.
    // paymentService.refundForCancellation 자체는 @Async라 이 메서드는(그리고 이걸 부른
    // cancelOrder/updateOrderStatus도) 토스 API 응답을 기다리지 않고 바로 반환된다 — 실제
    // 환불 시도는 별도 스레드에서 이어서 처리됨(PaymentService 참고).
    private void refundIfPaidByToss(Long orderId, boolean wasPaid, String reason) {
        // 결제 없이 생성된 데모 주문(PENDING) 취소는 애초에 환불할 것도, 대기중으로 표시해둘
        // 것도 없다 — 커밋 후 별도 트랜잭션을 여는 콜백 자체를 등록하지 않아 그 오버헤드도 없앤다.
        if (!wasPaid) return;

        // 이 표시는 취소를 담은 바로 그 트랜잭션 안에서 함께 커밋되어야만 의미가 있다(그래야
        // "환불 필요"라는 사실이 취소와 원자적으로 함께 남는다) — 그래서 REQUIRES_NEW로 분리할
        // 수 없다. 이 try/catch는 애플리케이션 코드에서 던진 예외가 이 메서드 밖으로 새는 것만
        // 막을 뿐, 진짜 DB/영속성 계층 예외(커넥션 끊김, 데드락 등)라면 Hibernate가 이미 그
        // 트랜잭션을 rollback-only로 표시해버린 뒤라 여기서 잡아도 바깥 트랜잭션(취소 자체)의
        // 커밋 실패까지 막을 수는 없다 — 이런 경우 취소 자체가 실패로 응답되지만, 재고 복구나
        // 취소 상태 없이 전부 원자적으로 롤백되므로 어중간한 상태 없이 안전하게 실패하고, 사용자는
        // 다시 취소를 시도하면 된다. PaymentService의 markLinked와 동일한 종류의, 받아들인 한계.
        try {
            int marked = paymentService.markRefundPendingForOrder(orderId);
            if (marked == 0) {
                // wasPaid인데 연결된 결제가 하나도 없음 — 주문 생성 시 markLinked가 드물게
                // 실패해 결제-주문 연결이 아예 안 남아있던 경우(OrderService.createOrder 주석
                // 참고). 실제로는 결제가 됐는데 자동으로 환불할 방법이 없는 상황이므로 여기서
                // 바로 알린다(뒤이은 refundForCancellation은 이 경우 할 일이 없어 조용히 끝남).
                log.error("결제 완료 상태였던 주문이 취소됐는데 연결된 결제 기록을 찾지 못함 — " +
                        "수동으로 토스 결제 내역을 확인해 환불이 필요합니다 (orderId={})", orderId);
            }
        } catch (Exception e) {
            log.error("환불 대기중 표시 실패 (orderId={})", orderId, e);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // paymentService.refundForCancellation은 @Async라 정상적인 경우 즉시 반환되지만,
                // 스레드풀이 꽉 차 작업 자체를 받아주지 못하면(TaskRejectedException) 그 예외는
                // 비동기로 처리되지 않고 이 자리에서 바로 던져진다. afterCommit에서 던진 예외는
                // 트랜잭션이 이미 커밋된 뒤에도 그대로 컨트롤러까지 새어나가(스프링이 커밋 자체는
                // 취소하지 않음) "주문 취소는 이미 성공했는데 응답은 500"이라는 앞뒤가 안 맞는
                // 결과를 만든다 — 그래서 여기서 반드시 잡아야 한다.
                try {
                    paymentService.refundForCancellation(orderId, reason);
                } catch (Exception e) {
                    log.error("환불 작업 등록 실패 (orderId={}) — 환불 대기중 상태로 남아있어 수동 확인 필요", orderId, e);
                }
            }
        });
    }

    // 취소된 주문의 상품별 재고를 원자적으로 복구한다. 조회 후 다시 쓰는 방식이면 같은
    // 상품이 포함된 두 주문이 동시에 취소될 때 한쪽 복구분이 유실될 수 있어(재고 차감
    // 오버셀 방지와 동일한 이유) increaseStock(원자적 UPDATE)으로 처리한다.
    private void restoreStock(Long orderId, List<OrderItem> items) {
        for (OrderItem item : items) {
            int affected = productRepository.increaseStock(item.getProduct().getId(), item.getQuantity());
            if (affected == 0) {
                // 복구 대상 상품을 못 찾은 경우(현재는 상품을 하드 삭제하지 않아 일어날 수
                // 없지만) 일부 상품만 복구되고 주문은 취소된 채로 남는 반쪽짜리 상태를
                // 만들지 않기 위해, 조용히 넘어가지 않고 던져서 취소 자체를 롤백시킨다.
                throw new IllegalStateException(
                        "재고 복구 대상 상품을 찾지 못했습니다 (orderId=" + orderId + ", productId=" + item.getProduct().getId() + ")");
            }
        }
    }

    // 관리자 - 전체 주문 목록 조회
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(OrderResponseDto::from)
                .collect(Collectors.toList());
    }

    private static final java.util.Set<String> VALID_ORDER_STATUSES = java.util.Set.of(
            "PENDING", "PAID", "PREPARING", "SHIPPED", "DELIVERED", "CANCELLED");

    // 관리자 - 주문 상태 변경
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, OrderRequestDto.UpdateStatus dto) {
        if (!VALID_ORDER_STATUSES.contains(dto.getStatus())) {
            throw new IllegalArgumentException("올바르지 않은 주문 상태입니다.");
        }

        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if ("CANCELLED".equals(dto.getStatus())) {
            // 재고는 주문 생성 시점에 이미 차감돼 있으므로, 취소가 아니던 주문이 취소로
            // 바뀔 때(회원 본인 취소 cancelOrder()와 동일하게) 재고를 복구해야 함.
            // CANCELLED로의 전이 자체를 원자적 조건부 UPDATE로 처리해서, 같은 주문에 대한
            // 상태변경 요청이 동시에 여러 번 들어와도(관리자 이중클릭, 회원 취소와 겹침 등)
            // 실제로 전이시킨 단 하나의 요청만 재고를 복구하도록 한다.
            List<OrderItem> items = new ArrayList<>(orders.getOrderItems());
            // 원자적 UPDATE(cancelIfNotCancelled)로 실제 상태를 바꾸기 전, 이 주문이 결제된 적이
            // 있었는지를 미리 기억해둔다. 현재 status가 "PAID"인지가 아니라 paidAt이 찍혀있는지로
            // 판단해야 한다 — PAID 이후 PREPARING/SHIPPED 등으로 더 진행된 주문을 관리자가 취소할
            // 수도 있는데, 그 경우 현재 status는 더 이상 "PAID"가 아니지만 결제는 분명히 됐던
            // 것이므로 여전히 환불 대상이다. PENDING(결제 없는 데모 주문)만 paidAt이 비어있다.
            boolean wasPaid = orders.getPaidAt() != null;
            int updated = orderRepository.cancelIfNotCancelled(orderId);
            if (updated > 0) {
                restoreStock(orderId, items);
                refundIfPaidByToss(orderId, wasPaid, "관리자에 의한 주문 취소");
            }
        } else {
            // 이 전이도 CANCELLED가 아닌 주문에서만 원자적으로 적용한다 — 그렇지 않으면
            // 회원이 막 취소해서 재고까지 복구된 주문을, 관리자의 다른 상태변경 요청이
            // 거의 동시에 덮어써서 "재고는 복구됐는데 주문은 취소 아님"으로 남을 수 있다.
            orderRepository.updateStatusIfNotCancelled(orderId, dto.getStatus());
        }

        // 위에서 원자적 UPDATE를 거쳤을 수 있어(영속성 컨텍스트가 비워짐) 최신 상태를
        // 다시 조회해서 응답한다 — 응답 조립을 "컨텍스트가 비워지기 전"에 해둬야 하는
        // 순서 의존적인 코드로 만들지 않기 위한 것 (실제로 그 순서를 놓쳐서
        // LazyInitializationException이 나는 걸 겪은 뒤 이 방식으로 정리함).
        Orders fresh = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        return OrderResponseDto.from(fresh);
    }
}