package com.forme.shop.payment.service;

import com.forme.shop.order.entity.Orders;
import com.forme.shop.payment.TossConfig;
import com.forme.shop.payment.entity.Payment;
import com.forme.shop.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final TossConfig tossConfig;

    // 결제 승인 성공 직후 기록 — 이 시점 이후로는 주문 생성이 무슨 이유로 실패하든
    // "카드는 결제됐다"는 사실이 DB에 남아있어야 함
    @Transactional
    public void recordConfirmed(String paymentKey, String tossOrderId, int amount) {
        // 같은 paymentKey로 이미 기록이 있으면(승인 응답 재처리 등) 새로 만들지 않음
        if (paymentRepository.findByPaymentKey(paymentKey).isPresent()) return;
        paymentRepository.save(Payment.builder()
                .paymentKey(paymentKey)
                .tossOrderId(tossOrderId)
                .amount(amount)
                .status("CONFIRMED")
                .build());
    }

    // 결제 승인 기록이 있고 아직 어떤 주문에도 연결되지 않은 "CONFIRMED" 상태인지 확인
    public boolean isConfirmed(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .map(p -> "CONFIRMED".equals(p.getStatus()))
                .orElse(false);
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
    // 반환값: 환불(취소) API 호출까지 성공했으면 true, 그마저 실패해 수동 확인이 필요하면 false
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean refundAndMarkFailed(String paymentKey, String reason) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey).orElse(null);
        if (payment == null) return true; // 승인 기록 자체가 없으면 환불할 결제도 없음

        try {
            String encodedKey = Base64.getEncoder()
                    .encodeToString((tossConfig.getSecretKey() + ":").getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encodedKey);

            Map<String, Object> body = Map.of("cancelReason", "주문 생성 실패로 인한 자동 취소");

            new RestTemplate().postForEntity(
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
