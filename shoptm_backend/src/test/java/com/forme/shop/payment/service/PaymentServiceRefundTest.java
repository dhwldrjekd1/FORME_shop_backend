package com.forme.shop.payment.service;

import com.forme.shop.payment.TossConfig;
import com.forme.shop.payment.entity.Payment;
import com.forme.shop.payment.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// 실제 토스 API를 부를 수 없는(또는 불러서는 안 되는) 환경에서 callTossCancelAndMark()의
// 예외 타입 분기(HttpStatusCodeException=확실한 실패 vs 그 외=응답 자체를 못 받음)가 올바른
// Payment.status로 이어지는지만 검증한다. static final RestTemplate을 리플렉션으로 모킹해
// 네트워크 호출 자체가 전혀 일어나지 않게 한다.
class PaymentServiceRefundTest {

    private PaymentRepository paymentRepository;
    private PaymentService paymentService;
    private RestTemplate mockRestTemplate;
    private Field restTemplateField;
    private Object staticFieldBase;
    private long staticFieldOffset;
    private RestTemplate originalRestTemplate;

    // static final 필드라 일반 리플렉션으로는 값을 바꿀 수 없어(JDK가 컴파일 타임에 인라인하지
    // 않는 참조형 final이라도 Field.set은 거부함) Unsafe로 메모리 위치에 직접 써서 실제 토스
    // API를 호출하지 않는 모킹된 RestTemplate으로 테스트 중에만 바꿔치기한다.
    private static Unsafe getUnsafe() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    @BeforeEach
    void setUp() throws Exception {
        paymentRepository = mock(PaymentRepository.class);
        TossConfig tossConfig = new TossConfig();
        paymentService = new PaymentService(paymentRepository, tossConfig);

        restTemplateField = PaymentService.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        originalRestTemplate = (RestTemplate) restTemplateField.get(null);

        Unsafe unsafe = getUnsafe();
        staticFieldBase = unsafe.staticFieldBase(restTemplateField);
        staticFieldOffset = unsafe.staticFieldOffset(restTemplateField);

        mockRestTemplate = mock(RestTemplate.class);
        unsafe.putObject(staticFieldBase, staticFieldOffset, mockRestTemplate);
    }

    @AfterEach
    void tearDown() throws Exception {
        getUnsafe().putObject(staticFieldBase, staticFieldOffset, originalRestTemplate);
    }

    private boolean invokeCallTossCancelAndMark(Payment payment) throws Exception {
        Method method = PaymentService.class.getDeclaredMethod(
                "callTossCancelAndMark", Payment.class, String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(paymentService, payment, "환불 사유", "환불 사유");
    }

    @Test
    void 토스가_응답으로_거부하면_종결상태인_REFUND_FAILED로_기록한다() throws Exception {
        Payment payment = Payment.builder()
                .paymentKey("key1")
                .tossOrderId("order1")
                .amount(1000)
                .status("REFUND_PENDING")
                .build();

        when(mockRestTemplate.postForEntity(any(String.class), any(), eq(Map.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST, "Bad Request", null, null, null));

        boolean result = invokeCallTossCancelAndMark(payment);

        assertFalse(result);
        assertEquals("REFUND_FAILED", payment.getStatus());
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void 응답자체를_못받으면_종결상태가_아닌_REFUND_UNKNOWN으로_기록한다() throws Exception {
        Payment payment = Payment.builder()
                .paymentKey("key2")
                .tossOrderId("order2")
                .amount(2000)
                .status("REFUND_PENDING")
                .build();

        when(mockRestTemplate.postForEntity(any(String.class), any(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));

        boolean result = invokeCallTossCancelAndMark(payment);

        assertFalse(result);
        assertEquals("REFUND_UNKNOWN", payment.getStatus());
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void REFUND_UNKNOWN_상태는_종결상태가_아니라서_환불이_다시_시도되면_API를_한번더_호출한다() throws Exception {
        Payment payment = Payment.builder()
                .paymentKey("key3")
                .tossOrderId("order3")
                .amount(3000)
                .status("REFUND_UNKNOWN")
                .build();

        when(mockRestTemplate.postForEntity(any(String.class), any(), eq(Map.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(Map.of()));

        boolean result = invokeCallTossCancelAndMark(payment);

        assertTrue(result);
        assertEquals("REFUNDED", payment.getStatus());
        verify(mockRestTemplate, times(1)).postForEntity(any(String.class), any(), eq(Map.class));
    }

    @Test
    void 이미_종결상태면_API를_다시_호출하지_않는다() throws Exception {
        Payment payment = Payment.builder()
                .paymentKey("key4")
                .tossOrderId("order4")
                .amount(4000)
                .status("REFUNDED")
                .build();

        boolean result = invokeCallTossCancelAndMark(payment);

        assertTrue(result);
        verifyNoInteractions(mockRestTemplate);
        verify(paymentRepository, never()).save(any());
    }
}
