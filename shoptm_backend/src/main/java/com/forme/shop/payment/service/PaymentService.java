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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
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

        // 이미 환불 처리(성공/실패)가 끝난 결제면 다시 취소 API를 부르지 않는다.
        // (동시에 실패한 두 요청이 같은 결제를 동시에 환불 처리하려는 경우, 두 번째 호출이
        // 이미 성공한 REFUNDED 상태를 REFUND_FAILED로 잘못 덮어쓰는 걸 방지)
        if (TERMINAL_STATUSES.contains(payment.getStatus())) {
            return "REFUNDED".equals(payment.getStatus());
        }

        try {
            String encodedKey = Base64.getEncoder()
                    .encodeToString((tossConfig.getSecretKey() + ":").getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encodedKey);

            Map<String, Object> body = Map.of("cancelReason", "주문 생성 실패로 인한 자동 취소");

            restTemplate.postForEntity(
                    "https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            payment.setStatus("REFUNDED");
            payment.setFailReason(reason);
            return true;
        } catch (Exception e) {
            log.error("결제 자동 환불 실패 - paymentKey={}, reason={}", paymentKey, reason, e);
            payment.setStatus("REFUND_FAILED");
            payment.setFailReason(reason + " / 자동 환불 시도도 실패: " + e.getMessage());
            return false;
        }
    }
}
