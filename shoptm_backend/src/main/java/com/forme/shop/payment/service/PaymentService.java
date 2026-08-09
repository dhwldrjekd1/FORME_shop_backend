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
    public void recordConfirmed(String paymentKey, String tossOrderId, int amount) {
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
                    .status("CONFIRMED")
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시에 두 번 승인 처리가 들어온 경우 — 다른 요청이 먼저 기록을 남긴 것이므로
            // 정상 상황으로 보고 무시한다 (이미 CONFIRMED 기록은 존재함)
            log.warn("결제 승인 기록 중복 저장 시도 무시 - paymentKey={}", paymentKey);
        }
    }

    // 결제 승인 기록이 있고 아직 어떤 주문에도 연결되지 않은 "CONFIRMED" 상태일 때만 반환
    public Optional<Payment> findConfirmed(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .filter(p -> "CONFIRMED".equals(p.getStatus()));
    }

    // 주문 생성 성공 시 결제 기록을 해당 주문에 연결
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
