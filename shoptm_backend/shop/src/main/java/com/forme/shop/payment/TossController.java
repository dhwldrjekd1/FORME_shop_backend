package com.forme.shop.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class TossController {

    private final TossConfig tossConfig;

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

            return ResponseEntity.ok(Map.of("success", true, "data", response.getBody()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
