package com.forme.shop.payment.service;

import com.forme.shop.order.entity.Orders;
import com.forme.shop.payment.TossConfig;
import com.forme.shop.payment.entity.Payment;
import com.forme.shop.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    // 토스 취소 API 호출용 — 커넥션/응답 타임아웃을 명시해, 외부 API가 느려져도
    // (특히 REQUIRES_NEW 트랜잭션 안에서 DB 커넥션을 오래 붙잡고 있지 않도록) 무한정 대기하지 않게 함
    private static final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    private static final Set<String> TERMINAL_STATUSES = Set.of("REFUNDED", "REFUND_FAILED");

    private final PaymentRepository paymentRepository;
    private final TossConfig tossConfig;

    // 결제 승인 성공 직후 기록 — 이 시점 이후로는 주문 생성이 무슨 이유로 실패하든
    // "카드는 결제됐다"는 사실이 DB에 남아있어야 함
    @Transactional
    public void recordConfirmed(String paymentKey, String tossOrderId, int amount, String memberEmail) {
        // 같은 paymentKey로 이미 기록이 있으면(승인 응답 재처리 등) 새로 만들지 않음
        if (paymentRepository.findByPaymentKey(paymentKey).isPresent()) return;
        try {
            // saveAndFlush로 즉시 INSERT를 실행해, 동시에 두 번 호출돼 unique 제약을 어기는 경우
            // 그 예외를 이 메서드 안에서 바로 잡아낼 수 있게 한다 (그냥 save는 커밋 시점까지
            // 지연될 수 있어 이 try/catch를 빠져나간 뒤에야 터질 수 있음)
            paymentRepository.saveAndFlush(Payment.builder()
                    .paymentKey(paymentKey)
                    .tossOrderId(tossOrderId)
                    .amount(amount)
                    .memberEmail(memberEmail)
                    .status("CONFIRMED")
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시에 두 번 승인 처리가 들어온 경우 — 다른 요청이 먼저 기록을 남긴 것이므로
            // 정상 상황으로 보고 무시한다 (이미 CONFIRMED 기록은 존재함)
            log.warn("결제 승인 기록 중복 저장 시도 무시 - paymentKey={}", paymentKey);
        }
    }

    // 이 결제를 "지금 이 요청이" 주문 생성에 쓰겠다고 선점한다. 같은 paymentKey로 동시에
    // 들어온 다른 요청(더블클릭, 네트워크 재시도)은 선점에 실패해 false를 받는다 — 그게 바로
    // 하나의 결제로 주문이 두 번 만들어지는 것을 막는 지점(멱등성의 핵심).
    // REQUIRES_NEW로 즉시 커밋시켜 이 행의 락을 바로 풀어준다 — 그렇지 않으면, 이 선점을
    // 호출한 주문 생성이 나중에 실패해 refundAndMarkFailed(마찬가지로 REQUIRES_NEW)를 부를 때
    // 같은 행에 대한 락을 서로 기다리며 스스로 교착 상태에 빠질 수 있다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimForOrderCreation(String paymentKey) {
        return paymentRepository.claimIfConfirmed(paymentKey) > 0;
    }

    // 선점에 성공한 직후 그 결제 기록을 조회 (선점 UPDATE는 벌크 연산이라 영속성 컨텍스트가
    // 비워지므로, DB에서 다시 읽어야 방금 바뀐 status="PROCESSING"과 실제 승인 금액을 얻을 수 있음)
    public Payment getByPaymentKey(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new IllegalStateException("결제 승인 기록을 찾을 수 없습니다: " + paymentKey));
    }

    // 선점(claim) 시도 전에 소유자만 미리 확인하기 위한 조회 — 기록이 없으면 예외 대신 null을
    // 반환한다. 선점부터 하고 소유자가 다르면 되돌리는(release) 순서로 짜면, 그 사이의 아주 짧은
    // 순간이라도 다른 요청(진짜 소유자의 동시 요청 등)이 이 결제를 선점하지 못해 실패하는 경쟁
    // 상태가 생긴다 — 소유자 확인을 선점보다 먼저 해서 그 경쟁 자체를 없앤다.
    public Payment findByPaymentKeyOrNull(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey).orElse(null);
    }

    // 선점에 실패했을 때, 이미 이 결제로 주문이 만들어져 있다면(재시도/더블클릭) 그 주문을 그대로
    // 돌려주기 위해 조회한다 — 새 주문을 또 만드는 대신 원래 요청의 결과를 그대로 재현하는 것
    public Optional<Orders> findLinkedOrder(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .filter(p -> "LINKED".equals(p.getStatus()))
                .map(Payment::getOrders);
    }

    // 주문 생성 성공 시 결제 기록을 해당 주문에 연결.
    // REQUIRES_NEW로 분리할 수 없음(claimForOrderCreation/refundAndMarkFailed와 달리) —
    // 아직 커밋 전인 주문을 참조하는 FK 쓰기라서, 별도 트랜잭션에서는 그 주문 행이 안 보여
    // FK 제약 위반으로 실패한다(실제로 시도해보고 확인함). 그래서 주문을 만든 바깥 트랜잭션에
    // 그대로 합류(REQUIRED)해야 하며, 커밋 시점 실패로부터의 완전한 격리는 포기하는 대신
    // OrderService의 try/catch로 최소한 예외가 새는 것만 막는다.
    @Transactional
    public void markLinked(String paymentKey, Orders orders) {
        paymentRepository.findByPaymentKey(paymentKey).ifPresent(payment -> {
            payment.setOrders(orders);
            payment.setStatus("LINKED");
        });
    }

    // 주문 생성이 실패했을 때, 이미 승인된 결제를 토스 취소 API로 자동 환불한다.
    // REQUIRES_NEW: 주문 생성 트랜잭션이 실패해서 롤백되더라도, 이 환불 처리와 상태 기록은
    // 별도 트랜잭션으로 반드시 커밋되어야 함 (그래야 "카드 결제됨 + 아무 기록 없음" 상태가 안 생김)
    // 반환값: 환불(취소) API 호출까지 성공했으면(혹은 이미 환불된 상태였으면) true,
    // 그마저 실패해 수동 확인이 필요하면 false
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean refundAndMarkFailed(String paymentKey, String reason) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey).orElse(null);
        if (payment == null) return true; // 승인 기록 자체가 없으면 환불할 결제도 없음
        return callTossCancelAndMark(payment, "주문 생성 실패로 인한 자동 취소", reason);
    }

    // 주문 취소를 커밋하는 바로 그 트랜잭션 안에서 호출해야 한다(별도 트랜잭션이 아님 — 이
    // 메서드 자체엔 일부러 @Transactional을 안 붙여서, 호출한 쪽의 진행 중인 트랜잭션에 자연스럽게
    // 합류해 그 트랜잭션과 함께 원자적으로 커밋/롤백되게 함). 연결된 결제를 "환불 대기중"으로
    // 미리 표시해 두는 용도로, 실제 토스 취소 API 호출(refundForCancellation, 커밋 후 별도 호출)
    // 자체가 프로세스 종료 등으로 아예 실행되지 못하더라도 "환불이 필요한 상태"라는 사실만은
    // 주문 취소와 함께 이미 DB에 남아 유실되지 않도록 하기 위함 (PaymentRepository 참고).
    // 반환값(영향받은 행 수)이 0이면 연결된 결제 자체가 없었다는 뜻 — 호출하는 쪽에서 이걸로
    // "결제된 주문인데 연결된 결제 기록이 없는" 이상 상황을 그 자리에서 바로 알 수 있다.
    public int markRefundPendingForOrder(Long orderId) {
        return paymentRepository.markRefundPendingForOrder(orderId);
    }

    // PAID 상태였던 주문이 취소(회원 본인 취소 또는 관리자의 상태 변경)됐을 때, 실제로 결제된
    // 돈을 토스 취소 API로 환불한다. 이전에는 주문 취소 시 재고만 복구하고 결제 자체는 그대로
    // 남아있어, 취소된 주문의 카드 대금이 고객에게 돌아가지 않는 문제가 있었다.
    // 호출하는 쪽(OrderService)이 이미 주문 취소 트랜잭션이 커밋된 뒤(afterCommit)에만 이
    // 메서드를 부르고, 그와 동시에 같은 트랜잭션 안에서 연결된 결제를 "환불 대기중"으로
    // 미리 표시(markRefundPendingForOrder)해뒀으므로, 이 메서드 자체가 아예 실행되지 못하거나
    // (프로세스 종료 등) 아래에서 실패하더라도 "환불 필요"라는 사실 자체는 유실되지 않는다.
    // @Async: 토스 API 호출은 느려질 수 있는 외부 네트워크 호출인데, 이 메서드를 통째로
    // @Transactional로 감싸면 그 호출 동안 DB 커넥션을 계속 붙잡게 된다(REQUIRES_NEW는 특히
    // 별도 커넥션을 하나 더 잡아둠). 여러 건이 동시에 취소되는 상황에서 토스가 느려지면
    // 커넥션 풀이 이걸로 다 막힐 수 있어, 응답을 기다리는 요청 스레드/트랜잭션과 분리해
    // 별도 스레드(AsyncConfig의 refundExecutor)에서 처리한다 — 그래서 반환 타입도 void(호출한
    // 쪽은 결과를 기다리지 않음)이고, 아래 조회/저장은 각각 리포지토리 메서드 자체의 짧은
    // 트랜잭션에 맡길 뿐 이 메서드 전체를 감싸는 트랜잭션은 없다(그 사이 토스 호출 동안은
    // DB 커넥션이 전혀 잡혀있지 않음).
    // order_id에 DB 유니크 제약이 없어(PaymentRepository 참고) 한 주문에 결제가 이론적으로
    // 둘 이상 연결될 수도 있으므로, 하나만 골라 처리하고 끝내지 않고 연결된 결제 전부를 각각
    // 환불 시도한다.
    @Async("refundExecutor")
    public void refundForCancellation(Long orderId, String reason) {
        try {
            List<Payment> payments = paymentRepository.findByOrders_Id(orderId);
            // 연결된 결제가 없는 경우(markLinked가 드물게 실패한 경우 등)는 이미 markRefundPendingForOrder
            // 호출 시점(OrderService, 취소와 같은 트랜잭션 안)에서 감지해 로그를 남겼으므로 여기서 또
            // 중복으로 알리지 않는다.
            if (payments.isEmpty()) return;

            // 결제 하나 처리 중 예기치 못한 예외가 나더라도, 같은 주문에 연결된 나머지 결제까지
            // 통째로 건너뛰지 않도록 각각 독립적으로 시도한다.
            boolean allSucceeded = true;
            for (Payment payment : payments) {
                try {
                    if (!callTossCancelAndMark(payment, "주문 취소로 인한 환불", reason)) {
                        allSucceeded = false;
                    }
                } catch (Exception e) {
                    allSucceeded = false;
                    log.error("결제 환불 처리 중 예기치 못한 오류 (paymentKey={})", payment.getPaymentKey(), e);
                }
            }
            if (!allSucceeded) {
                log.error("주문 취소는 정상 처리됐으나 환불에 실패해 수동 확인이 필요함 (orderId={})", orderId);
            }
        } catch (Exception e) {
            log.error("주문 취소 환불 처리 중 오류 (orderId={})", orderId, e);
        }
    }

    // 토스 결제 취소(환불) API를 실제로 호출하고, 결과에 따라 Payment 상태를 갱신한다.
    // refundAndMarkFailed(주문 생성 실패, REQUIRES_NEW 트랜잭션 안에서 호출됨)와
    // refundForCancellation(주문 취소, 트랜잭션 없이 호출됨)이 공통으로 쓰기 때문에, 두 경우
    // 모두에서 변경 사항이 확실히 저장되도록 매번 명시적으로 save를 호출한다(트랜잭션이 있는
    // 경우엔 사실상 중복이지만 해가 되지 않고, 트랜잭션이 없는 경우엔 이게 없으면 엔티티
    // 변경이 그냥 버려진다).
    private boolean callTossCancelAndMark(Payment payment, String tossCancelReason, String storedReason) {
        // 이미 환불 처리(성공/실패)가 끝난 결제면 다시 취소 API를 부르지 않는다.
        // (동시에 들어온 두 요청이 같은 결제를 동시에 환불 처리하려는 경우, 두 번째 호출이
        // 이미 성공한 REFUNDED 상태를 REFUND_FAILED로 잘못 덮어쓰는 걸 방지)
        if (TERMINAL_STATUSES.contains(payment.getStatus())) {
            return "REFUNDED".equals(payment.getStatus());
        }

        // 아래 catch는 "토스 취소 API 호출 자체"의 실패만을 위한 것이다. 그 호출이 실제로
        // 성공한 뒤(=돈은 이미 환불된 뒤) 그 결과를 로컬에 저장하는 것까지 이 try 안에 함께
        // 두면, save()가(커넥션 끊김 등으로) 실패했을 때 이 catch로 떨어져 "환불 시도 자체가
        // 실패했다"는 REFUND_FAILED로 잘못 기록해버린다 — 실제로는 토스에 이미 환불된 돈을
        // "환불 안 됨"으로 기록해, 담당자가 그 잘못된 기록만 보고 중복 환불을 시도하게 만들
        // 수 있는 훨씬 나쁜 상태다. 그래서 저장은 성공/실패 각 경로에서 따로, 그 경로에
        // 맞는 상태로만 시도한다.
        try {
            String encodedKey = Base64.getEncoder()
                    .encodeToString((tossConfig.getSecretKey() + ":").getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encodedKey);

            Map<String, Object> body = Map.of("cancelReason", tossCancelReason);

            restTemplate.postForEntity(
                    "https://api.tosspayments.com/v1/payments/" + payment.getPaymentKey() + "/cancel",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
        } catch (Exception e) {
            log.error("결제 자동 환불 실패 - paymentKey={}, reason={}", payment.getPaymentKey(), storedReason, e);
            payment.setStatus("REFUND_FAILED");
            // 실패 사유는 사람이 나중에 DB를 보고 원인을 가늠하라고 남기는 참고용 텍스트일 뿐이라,
            // 길이를 줄여도 정보 손실의 실질적 피해가 없다. 반면 여기서 컬럼 길이(255)를 넘겨
            // 이 INSERT/UPDATE 자체가 실패하면, 환불 실패라는 사실 자체를 기록할 방법이 없어져
            // "환불도 안 됐는데 실패 기록도 없는" 최악의 상태가 된다 — 그걸 막기 위한 자름.
            payment.setFailReason(truncateToColumn(storedReason + " / 자동 환불 시도도 실패: " + e.getMessage()));
            try {
                paymentRepository.save(payment);
            } catch (Exception saveError) {
                log.error("환불 실패 기록 저장 자체도 실패함 (paymentKey={})", payment.getPaymentKey(), saveError);
            }
            return false;
        }

        // 여기 도달했다는 건 토스 취소 API 호출 자체는 성공했다는 뜻(=돈은 실제로 환불됨).
        // 이 저장이 실패하더라도 그 사실을 잘못 뒤집지 않는다 — 로컬 기록만 못 남겼을 뿐,
        // 환불 자체는 이미 실제로 일어난 것이므로 여전히 성공으로 보고한다.
        payment.setStatus("REFUNDED");
        payment.setFailReason(truncateToColumn(storedReason));
        try {
            paymentRepository.save(payment);
        } catch (Exception saveError) {
            log.error("토스 환불 자체는 성공했지만 그 결과를 로컬에 저장하는 데 실패함 — " +
                    "실제로는 REFUNDED 상태임에 주의 (paymentKey={})", payment.getPaymentKey(), saveError);
        }
        return true;
    }

    private static final int FAIL_REASON_MAX_LENGTH = 255;

    private String truncateToColumn(String text) {
        if (text == null || text.length() <= FAIL_REASON_MAX_LENGTH) return text;
        return text.substring(0, FAIL_REASON_MAX_LENGTH);
    }
}
