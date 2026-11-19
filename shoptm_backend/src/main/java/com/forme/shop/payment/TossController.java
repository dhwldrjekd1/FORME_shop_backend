package com.forme.shop.payment;

import com.forme.shop.common.security.SecurityUtil;
import com.forme.shop.payment.entity.Payment;
import com.forme.shop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class TossController {

    private static final Logger log = LoggerFactory.getLogger(TossController.class);

    // 토스 응답 자체를 못 받은 경우와, 받긴 했지만 이미 한 번 승인된 적 있는 paymentKey가
    // 4xx로 거부된 경우 둘 다 같은 "확정 불가" 상황이라 같은 문구를 쓴다 — 문자열 리터럴을
    // 두 곳에 따로 두면 나중에 문구를 바꿀 때 한쪽만 바뀌고 다른 쪽은 그대로 남을 수 있다.
    private static final String CONFIRM_RESULT_UNKNOWN_MESSAGE =
            "결제 승인 결과를 확인할 수 없습니다. 카드가 결제됐을 수 있으니 아래 결제번호를 보관하고 고객센터로 문의해주세요.";

    private final TossConfig tossConfig;
    private final PaymentService paymentService;

    // PaymentService.buildRestTemplate()과 동일한 이유로 타임아웃을 명시한다 — 여기선
    // 기본 RestTemplate(타임아웃 무제한)을 매 요청마다 새로 만들어 쓰고 있었는데, 이 호출은
    // (환불처럼 비동기가 아니라) 사용자의 요청 스레드 위에서 그대로 실행되기 때문에, 토스
    // 쪽이 응답을 안 주고 멈추면 그 서블릿 스레드가 무한정 묶여있게 된다. 장애 중 반복되면
    // 스레드 풀이 고갈될 수 있어 커넥션/응답 타임아웃을 둔다.
    private static final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    @GetMapping("/client-key")
    public ResponseEntity<?> getClientKey() {
        return ResponseEntity.ok(Map.of("clientKey", tossConfig.getClientKey()));
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody Map<String, Object> body) {
        // 요청 바디가 JSON 리터럴 null이면 Jackson이 body 자체를 null로 바인딩한다(역직렬화
        // 단계에서는 예외가 안 남) — 그 상태로 body.get(...)을 부르면 NullPointerException이
        // 나서, 바로 아래 캐스팅 오류와 똑같은 종류의 문제(이 엔드포인트 특유의 400 응답
        // 모양을 못 지키고 GlobalExceptionHandler의 일반 500으로 새버림)가 생긴다(교차검증에서
        // 발견). 캐스팅 오류를 잡는 것과 같은 이유로 여기서 먼저 걸러낸다.
        if (body == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "요청 형식이 올바르지 않습니다."));
        }

        // paymentKey/orderId가 문자열이 아닌 값(예: 숫자)으로 오면 아래 캐스팅 자체가
        // ClassCastException을 던진다 — 이걸 이 블록 밖에서 처리하면(교차검증에서 발견)
        // GlobalExceptionHandler의 일반 500 처리로 넘어가 프론트가 기대하는 {success, message}
        // 모양이 아닌 전혀 다른 응답 바디가 나간다. 여기서 바로 잡아 이 엔드포인트 특유의
        // 400 응답 형태를 유지한다. 이 시점엔 토스를 호출하기도 전이라 아래의 "응답을 못
        // 받음" 분기와 섞이면 안 되므로 별도로 분리해둔다.
        String paymentKey;
        String orderId;
        Object amount;
        try {
            paymentKey = (String) body.get("paymentKey");
            orderId = (String) body.get("orderId");
            amount = body.get("amount");
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "요청 형식이 올바르지 않습니다."));
        }

        // paymentKey/orderId/amount 중 하나라도 비어있으면 Map.of(...)가 null 값에 그대로
        // NullPointerException을 던진다 — 그걸 아래 catch(Exception e)가 "토스 응답을 못 받음"
        // 분기로 잘못 잡아서, 토스에 요청 자체를 보내보지도 않았는데 "카드가 결제됐을 수
        // 있다"는 안내와 함께 CONFIRM_UNKNOWN 기록까지 남길 뻔했다(교차검증에서 발견). 토스를
        // 부르기 전에 미리 걸러 그 오분류 자체를 없앤다.
        if (paymentKey == null || paymentKey.isBlank() || orderId == null || orderId.isBlank() || amount == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "결제 승인에 필요한 정보가 누락되었습니다."));
        }

        // 아래 두 catch는 "토스 승인 API 호출 자체"의 실패만을 위한 것이다 — PaymentService의
        // callTossCancelAndMark(환불)와 정확히 같은 이유로, 토스가 이 요청 자체를 확인하고
        // 거부했다고 확신할 수 있는지(4xx — 돈이 안 움직였다고 안전하게 단정 가능) 아닌지
        // (응답을 못 받았거나 5xx — 토스 내부적으로는 이미 승인을 완료했을 가능성을 배제할 수
        // 없음)를 구분한다. 여기가 환불 쪽보다 더 중요한 이유: 이 경로는 돈이 "처음" 움직이는
        // 지점이라, 여기서 확정된 실패로 잘못 단정하면 환불 쪽과 달리 Payment 행 자체가 전혀
        // 안 만들어져 그 결제가 시스템 어디에도 흔적이 안 남는다(다른 방향에서 버그를 찾다가
        // 발견).
        ResponseEntity<Map> response;
        try {
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

            response = restTemplate.postForEntity(
                    TossConfig.CONFIRM_URL,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );
        } catch (HttpClientErrorException e) {
            // 4xx — 이 paymentKey로 Payment 행이 전혀 없는 상태(=이번이 이 키에 대한 첫
            // 승인 시도)에서 받는 4xx라면, 토스가 이 요청 자체를 확인하고 거부했다는 뜻(잘못된
            // paymentKey/금액 불일치 등)이라 카드가 결제되지 않았다고 확신해도 안전하다.
            // 하지만 행이 이미 존재한다면(CONFIRM_UNKNOWN은 물론, 심지어 CONFIRMED여도) 얘기가
            // 다르다 — 둘 다 "이 결제가 한 번은 승인까지 간 적이 있다"는 뜻인데, 그 뒤 같은
            // paymentKey로 승인을 또 시도하면 토스가 "이미 처리된 결제"라는 뜻으로 4xx를
            // 돌려줄 수 있다. 이 경우의 4xx는 "거부"가 아니라 오히려 "이미 성공했다"는
            // 신호에 가깝다(교차검증에서 발견 — 처음엔 CONFIRM_UNKNOWN만 확인했는데, 이미
            // CONFIRMED로 정상 기록된 결제가 재시도로 다시 4xx를 받는 경우까지는 못 잡아서
            // 실제로는 성공한 결제를 "거부됨"으로 잘못 안내할 수 있었음). 이 구분을 정확히
            // 하려면 토스의 구체적인 에러 코드를 봐야 하는데, 이 코드베이스에 그걸 파싱한
            // 선례가 없고 검증되지 않은 토스 API 세부사항을 추측하는 셈이라, 대신 "이
            // paymentKey로 행 자체가 존재하는지"만 확인해 존재하면 확정된 실패로 단정하지
            // 않고 불확실한 상태로 안내한다.
            // findByPaymentKeyOrNull도 DB 조회라 그 자체가(커넥션 풀 고갈 등으로) 실패할 수
            // 있다 — 이 catch 블록 밖으로 새면 GlobalExceptionHandler의 일반 500 처리로
            // 넘어가 프론트가 기대하는 {success, message} 모양이 아닌 응답이 나간다. 토스가
            // 이미 4xx로 거부한 상황이니, 이 확인 자체가 실패해도 "정상적으로 거부됨"으로
            // 안전하게 처리한다(existing == null과 동일하게 취급).
            Payment existing;
            try {
                existing = paymentService.findByPaymentKeyOrNull(paymentKey);
            } catch (Exception lookupError) {
                log.error("4xx 거부 응답 처리 중 기존 결제 기록 조회 자체도 실패함 (paymentKey={})", paymentKey, lookupError);
                existing = null;
            }
            if (existing != null) {
                log.error("결제 승인 거부(4xx) 받았지만 이 paymentKey는 이미 한 번 승인 기록이 있었음 — " +
                        "토스가 '이미 처리됨'을 거부로 돌려줬을 가능성이 있어 확정된 실패로 단정하지 않음. " +
                        "토스 대시보드에서 직접 확인 필요 - paymentKey={}, status={}, body={}",
                        paymentKey, e.getStatusCode(), e.getResponseBodyAsString(), e);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", CONFIRM_RESULT_UNKNOWN_MESSAGE,
                        "paymentKey", paymentKey
                ));
            }
            log.error("결제 승인 실패(토스가 거부) - paymentKey={}, status={}, body={}",
                    paymentKey, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "결제 승인이 거부되었습니다."));
        } catch (Exception e) {
            // 응답 자체를 못 받음(타임아웃) 또는 5xx — 토스 쪽에서는 실제로 이미 승인을
            // 완료했을 수 있어 "확정된 실패"로 단정하지 않는다. amount는 클라이언트가 보낸
            // 값이라 금액 자체는 신뢰하지 않지만, 이 paymentKey로 기록만은 남겨둬야 관리자가
            // 토스 대시보드에서 실제 처리 여부를 찾아 확인할 수 있다 — 기록조차 없으면 이
            // 결제는 영영 찾을 방법이 없어진다.
            log.error("결제 승인 결과를 확인할 수 없음(토스 응답 없음, 타임아웃/서버 오류 등) — " +
                    "토스 쪽에서 실제로는 이미 승인됐을 수 있어 실패로 단정하지 않음. " +
                    "토스 대시보드에서 직접 확인 필요 - paymentKey={}, orderId={}",
                    paymentKey, orderId, e);
            try {
                paymentService.recordConfirmAmbiguous(paymentKey, orderId, parseAmount(amount), SecurityUtil.getCurrentEmail());
            } catch (Exception saveError) {
                log.error("승인 결과 확인불가 기록 저장 자체도 실패함 (paymentKey={})", paymentKey, saveError);
            }
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", CONFIRM_RESULT_UNKNOWN_MESSAGE,
                    "paymentKey", paymentKey
            ));
        }

        // 클라이언트가 보낸 amount가 아니라, 토스가 실제로 승인했다고 응답한 totalAmount를
        // 신뢰 가능한 결제 금액으로 사용한다. 이 값을 그대로 주문 생성(createOrder)의
        // paidAmount로 넘겨서, 서버가 계산한 실제 주문 금액과 대조하게 한다.
        // totalAmount가 없으면 클라이언트 값으로 대체하지 않고 승인 실패로 처리한다.
        Object confirmedAmount = response.getBody() != null ? response.getBody().get("totalAmount") : null;
        // totalAmount는 이후 "신뢰 가능한 금액"으로 그대로 쓰이므로, 숫자가 아닌 값이면
        // parseAmount의 안전한 기본값(0)으로 조용히 넘어가는 대신(그러면 실제로는 결제된
        // 카드가 금액 0원짜리 기록으로 남아버림) null과 동일하게 승인 실패로 처리한다
        // (교차검증에서 발견).
        if (!(confirmedAmount instanceof Number)) {
            log.error("결제 승인 응답의 totalAmount가 숫자가 아님 - paymentKey={}, value={}", paymentKey, confirmedAmount);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "결제 승인 응답에 금액 정보가 없습니다."));
        }

        // 결제 승인이 여기서 이미 확정(카드 결제 완료)됐으므로, 이후 주문 생성이
        // 어떤 이유로든 실패해도 "결제는 됐는데 기록이 없는" 상태가 남지 않도록 즉시 저장.
        // paymentKey는 그대로 응답에 포함해 프론트가 주문 생성 요청에 함께 실어 보내도록 함.
        // 이 결제를 실제로 승인받은 사람이 누구인지 기록해둔다 — 이후 주문 생성 시 이 값과
        // 대상 회원이 같은지 확인해, paymentKey만 알아내서 남의 결제로 주문을 가로채는 것을
        // 막는 데 쓴다(OrderService.createOrder 참고). 이 엔드포인트는 SecurityConfig에서
        // 이미 인증이 필요하므로 getCurrentEmail()은 항상 로그인한 사용자의 이메일을 반환한다.
        // 토스 승인 자체는 이미 성공했으므로(카드는 확실히 결제됨), 이 저장이 실패하더라도
        // 그 사실을 잘못 뒤집어 실패로 응답하지 않는다 — 그러면 사용자가 재시도해 이중결제로
        // 이어질 수 있다. 로그만 크게 남겨 수동 확인을 유도한다.
        try {
            // confirmedAmount는 바로 위에서 Number임을 이미 확인했으므로 parseAmount가
            // 안전하게 int로 변환한다(0으로 대체되는 일은 여기선 일어나지 않음).
            paymentService.recordConfirmed(
                    paymentKey, orderId, parseAmount(confirmedAmount), SecurityUtil.getCurrentEmail());
        } catch (Exception e) {
            log.error("토스 결제 승인 자체는 성공했지만 로컬 기록 저장에 실패함 — 수동 확인 필요 " +
                    "(paymentKey={}, amount={})", paymentKey, confirmedAmount, e);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "amount", confirmedAmount,
                "paymentKey", paymentKey,
                "data", response.getBody()
        ));
    }

    // 클라이언트가 보낸 amount는 JSON 역직렬화 과정에서 Integer/Double/Long 등 여러 숫자
    // 타입으로 들어올 수 있고, 이 값을 쓰는 곳(recordConfirmAmbiguous)은 금액을 신뢰해서
    // 계산에 쓰는 게 아니라 관리자가 참고할 단서로만 남기는 것이므로, 파싱이 안 되는
    // 경우에도 예외로 전체 흐름을 막지 않고 0으로 대체한다.
    private int parseAmount(Object amount) {
        if (amount instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(amount));
        } catch (Exception e) {
            return 0;
        }
    }
}
