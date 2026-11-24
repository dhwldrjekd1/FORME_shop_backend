package com.forme.shop.delivery.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// 배송 정보의 carrier/trackingNumber가 Delivery 컬럼 길이(각각 50)를 넘으면 DB가 거부하는
// DataIntegrityViolationException이 그대로 500으로 노출되던 문제를 고친 뒤 검증 — Spring의
// Validator 빈을 직접 호출하는 순수 검증 테스트(HTTP/DB 변경 없음).
class DeliveryRequestDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void Create_택배사가_51자면_거부된다() {
        var dto = new DeliveryRequestDto.Create();
        dto.setCarrier("가".repeat(51));
        Set<?> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void Create_택배사가_정확히_50자면_통과한다() {
        var dto = new DeliveryRequestDto.Create();
        dto.setCarrier("가".repeat(50));
        Set<?> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void Create_운송장번호가_51자면_거부된다() {
        var dto = new DeliveryRequestDto.Create();
        dto.setCarrier("CJ대한통운");
        dto.setTrackingNumber("1".repeat(51));
        Set<?> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void Create_운송장번호가_없어도_통과한다() {
        var dto = new DeliveryRequestDto.Create();
        dto.setCarrier("CJ대한통운");
        Set<?> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void Update_택배사가_51자면_거부된다() {
        var dto = new DeliveryRequestDto.Update();
        dto.setCarrier("가".repeat(51));
        dto.setStatus("READY");
        Set<?> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void Update_택배사가_공백뿐이고_51자여도_메시지가_하나로만_응답된다() {
        var dto = new DeliveryRequestDto.Update();
        dto.setCarrier(" ".repeat(51));
        dto.setStatus("READY");
        Set<ConstraintViolation<DeliveryRequestDto.Update>> violations = validator.validate(dto);
        long carrierViolations = violations.stream()
                .filter(v -> "carrier".equals(v.getPropertyPath().toString()))
                .count();
        assertEquals(1, carrierViolations);
    }

    @Test
    void Update_운송장번호만_51자여도_거부되고_상태만_바꾸는_요청은_그대로_통과한다() {
        var validDto = new DeliveryRequestDto.Update();
        validDto.setStatus("IN_TRANSIT");
        Set<?> validViolations = validator.validate(validDto);
        assertTrue(validViolations.isEmpty());

        var invalidDto = new DeliveryRequestDto.Update();
        invalidDto.setStatus("IN_TRANSIT");
        invalidDto.setTrackingNumber("1".repeat(51));
        Set<?> invalidViolations = validator.validate(invalidDto);
        assertFalse(invalidViolations.isEmpty());
    }
}
