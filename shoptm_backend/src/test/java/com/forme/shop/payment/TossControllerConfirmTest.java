package com.forme.shop.payment;

import com.forme.shop.payment.entity.Payment;
import com.forme.shop.payment.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// TossController.confirmPayment()의 분기 로직만 검증한다(실제 토스 API 호출 없음) —
// PaymentServiceRefundTest와 동일한 이유로 static final RestTemplate을 Unsafe로 모킹한다.
// 핵심 시나리오: ① 토스 응답을 못 받으면(타임아웃 등) 확정된 실패로 단정하지 않고
// CONFIRM_UNKNOWN 기록을 남김 — 예전엔 이 경우 Payment 행 자체가 전혀 안 만들어졌음
// (다른 방향에서 버그를 찾다가 발견). ② 이미 한 번 기록이 있는 paymentKey가 4xx로 거부되면
// "이미 처리됨"을 거부로 돌려받았을 가능성이 있어 확정된 실패로 단정하지 않음(교차검증에서
// 발견). ③ 승인 성공 시 토스가 실제로 돌려준 금액으로 CONFIRMED 기록.
class TossControllerConfirmTest {

    private PaymentService mockPaymentService;
    private TossController controller;
    private RestTemplate mockRestTemplate;
    private Field restTemplateField;
    private Object staticFieldBase;
    private long staticFieldOffset;
    private RestTemplate originalRestTemplate;

    private static Unsafe getUnsafe() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    @BeforeEach
    void setUp() throws Exception {
        mockPaymentService = mock(PaymentService.class);
        TossConfig tossConfig = new TossConfig();
        controller = new TossController(tossConfig, mockPaymentService);

        restTemplateField = TossController.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        originalRestTemplate = (RestTemplate) restTemplateField.get(null);

        Unsafe unsafe = getUnsafe();
        staticFieldBase = unsafe.staticFieldBase(restTemplateField);
        staticFieldOffset = unsafe.staticFieldOffset(restTemplateField);

        mockRestTemplate = mock(RestTemplate.class);
        unsafe.putObject(staticFieldBase, staticFieldOffset, mockRestTemplate);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer@test.com", null, List.of()));
    }

    @AfterEach
    void tearDown() throws Exception {
        getUnsafe().putObject(staticFieldBase, staticFieldOffset, originalRestTemplate);
        SecurityContextHolder.clearContext();
    }

    private Map<String, Object> body(String paymentKey, String orderId, Object amount) {
        Map<String, Object> m = new HashMap<>();
        m.put("paymentKey", paymentKey);
        m.put("orderId", orderId);
        m.put("amount", amount);
        return m;
    }

    @Test
    void 토스_응답을_못받으면_확정된_실패로_단정하지_않고_불확실_기록을_남긴다() {
        when(mockRestTemplate.postForEntity(any(String.class), any(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));

        ResponseEntity<?> response = controller.confirmPayment(body("key1", "order1", 10000));

        assertEquals(400, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals(false, responseBody.get("success"));
        assertEquals("key1", responseBody.get("paymentKey"));
        verify(mockPaymentService, times(1))
                .recordConfirmAmbiguous(eq("key1"), eq("order1"), eq(10000), eq("buyer@test.com"));
        verify(mockPaymentService, never()).recordConfirmed(any(), any(), anyInt(), any());
    }

    @Test
    void 처음보는_paymentKey가_4xx로_거부되면_확정된_거부로_안내한다() {
        when(mockPaymentService.findByPaymentKeyOrNull("key2")).thenReturn(null);
        when(mockRestTemplate.postForEntity(any(String.class), any(), eq(Map.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null, null, null));

        ResponseEntity<?> response = controller.confirmPayment(body("key2", "order2", 10000));

        assertEquals(400, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("결제 승인이 거부되었습니다.", responseBody.get("message"));
        verify(mockPaymentService, never()).recordConfirmAmbiguous(any(), any(), anyInt(), any());
    }

    @Test
    void 이미_기록이_있던_paymentKey가_4xx로_거부되면_확정된_실패로_단정하지_않는다() {
        Payment existing = Payment.builder().paymentKey("key3").tossOrderId("order3")
                .amount(10000).status("CONFIRM_UNKNOWN").build();
        when(mockPaymentService.findByPaymentKeyOrNull("key3")).thenReturn(existing);
        when(mockRestTemplate.postForEntity(any(String.class), any(), eq(Map.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null, null, null));

        ResponseEntity<?> response = controller.confirmPayment(body("key3", "order3", 10000));

        assertEquals(400, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue(((String) responseBody.get("message")).contains("확인할 수 없습니다"));
        assertEquals("key3", responseBody.get("paymentKey"));
    }

    @Test
    void 승인_성공시_토스가_돌려준_금액으로_CONFIRMED_기록한다() {
        Map<String, Object> tossResponseBody = Map.of("totalAmount", 25000);
        when(mockRestTemplate.postForEntity(any(String.class), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(tossResponseBody));

        // 클라이언트가 보낸 금액(1원)과 토스가 실제로 승인했다고 응답한 금액(25000원)이
        // 다른 경우 — 토스 쪽 금액을 신뢰해야 한다.
        ResponseEntity<?> response = controller.confirmPayment(body("key4", "order4", 1));

        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals(true, responseBody.get("success"));
        assertEquals(25000, responseBody.get("amount"));
        verify(mockPaymentService, times(1))
                .recordConfirmed(eq("key4"), eq("order4"), eq(25000), eq("buyer@test.com"));
    }

    @Test
    void 필수값이_누락되면_토스를_호출하지_않고_바로_거부한다() {
        ResponseEntity<?> response = controller.confirmPayment(body(null, "order5", 10000));

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(mockRestTemplate);
    }

    @Test
    void 요청_바디가_null이면_토스를_호출하지_않고_바로_거부한다() {
        ResponseEntity<?> response = controller.confirmPayment(null);

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(mockRestTemplate);
    }
}
