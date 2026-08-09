package com.forme.shop.payment;

import com.forme.shop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class TossController {

    private static final Logger log = LoggerFactory.getLogger(TossController.class);

    private final TossConfig tossConfig;
    private final PaymentService paymentService;

    @GetMapping("/client-key")
    public ResponseEntity<?> getClientKey() {
        return ResponseEntity.ok(Map.of("clientKey", tossConfig.getClientKey()));
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody Map<String, Object> body) {
        try {
            String paymentKey = (String) body.get("paymentKey");
            String orderId = (String) body.get("orderId");
            Object amount = body.get("amount");

            String encodedKey = Base64.getEncoder()
                    .encodeToString((tossConfig.getSecretKey() + ":").getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encodedKey);

            Map<String, Object> requestBody = Map.of(
                    "paymentKey", paymentKey,
                    "orderId", orderId,
                    "amount", amount
            );

            ResponseEntity<Map> response = new RestTemplate().postForEntity(
                    TossConfig.CONFIRM_URL,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // 클라이언트가 보낸 amount가 아니라, 토스가 실제로 승인했다고 응답한 totalAmount를
            // 신뢰 가능한 결제 금액으로 사용한다. 이 값을 그대로 주문 생성(createOrder)의
            // paidAmount로 넘겨서, 서버가 계산한 실제 주문 금액과 대조하게 한다.
            // totalAmount가 없으면 클라이언트 값으로 대체하지 않고 승인 실패로 처리한다.
            Object confirmedAmount = response.getBody() != null ? response.getBody().get("totalAmount") : null;
            if (confirmedAmount == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "결제 승인 응답에 금액 정보가 없습니다."));
            }

            // 결제 승인이 여기서 이미 확정(카드 결제 완료)됐으므로, 이후 주문 생성이
            // 어떤 이유로든 실패해도 "결제는 됐는데 기록이 없는" 상태가 남지 않도록 즉시 저장.
            // paymentKey는 그대로 응답에 포함해 프론트가 주문 생성 요청에 함께 실어 보내도록 함.
            paymentService.recordConfirmed(paymentKey, orderId, ((Number) confirmedAmount).intValue());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "amount", confirmedAmount,
                    "paymentKey", paymentKey,
                    "data", response.getBody()
            ));
        } catch (Exception e) {
            log.error("결제 승인 처리 실패", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "결제 승인 처리 중 오류가 발생했습니다."));
        }
    }
}
