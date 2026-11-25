package com.forme.shop.product.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// 상품 가격에 상한(1억원)이 없어서 수량/품목 수와 겹치면 int 오버플로로 이어질 수 있던 문제를
// 고친 뒤 검증 — Spring의 Validator 빈을 직접 호출하는 순수 검증 테스트(HTTP/DB 변경 없음).
class ProductRequestDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void Create_가격이_상한을_넘으면_거부된다() {
        var dto = new ProductRequestDto.Create();
        dto.setName("테스트상품");
        dto.setPrice(ProductRequestDto.MAX_PRICE + 1);
        dto.setStock(1);
        Set<ConstraintViolation<ProductRequestDto.Create>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> "price".equals(v.getPropertyPath().toString())));
    }

    @Test
    void Create_가격이_정확히_상한이면_통과한다() {
        var dto = new ProductRequestDto.Create();
        dto.setName("테스트상품");
        dto.setPrice(ProductRequestDto.MAX_PRICE);
        dto.setStock(1);
        Set<ConstraintViolation<ProductRequestDto.Create>> violations = validator.validate(dto);
        assertTrue(violations.stream().noneMatch(v -> "price".equals(v.getPropertyPath().toString())));
    }

    @Test
    void Create_할인전가격도_상한을_넘으면_거부된다() {
        var dto = new ProductRequestDto.Create();
        dto.setName("테스트상품");
        dto.setPrice(1000);
        dto.setStock(1);
        dto.setOriginalPrice(ProductRequestDto.MAX_PRICE + 1);
        Set<ConstraintViolation<ProductRequestDto.Create>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> "originalPrice".equals(v.getPropertyPath().toString())));
    }

    @Test
    void Update_가격이_상한을_넘으면_거부된다() {
        var dto = new ProductRequestDto.Update();
        dto.setPrice(ProductRequestDto.MAX_PRICE + 1);
        Set<ConstraintViolation<ProductRequestDto.Update>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> "price".equals(v.getPropertyPath().toString())));
    }
}
