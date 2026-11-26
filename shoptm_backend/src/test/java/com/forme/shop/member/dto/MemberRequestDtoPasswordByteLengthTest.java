package com.forme.shop.member.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// BCrypt가 72바이트를 넘는 비밀번호를 encode()에서 예외로 거부하는 문제(교차검증에서 발견)를
// @AssertTrue로 가입/수정 단계에서 미리 막았는지 확인. private 메서드 기반 제약이 Hibernate
// Validator에 실제로 인식되는지까지 함께 검증.
class MemberRequestDtoPasswordByteLengthTest {

    // 테스트 메서드마다(JUnit 5 기본 동작) 매번 새로 만들면 전체 분류 스캔+부트스트랩이 반복돼
    // 느려지므로(교차검증에서 발견), 클래스당 한 번만 만들어 공유한다.
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void Register_73바이트_ASCII_비밀번호는_거부된다() {
        MemberRequestDto.Register dto = new MemberRequestDto.Register();
        dto.setEmail("a@a.com");
        dto.setPassword("a".repeat(73));
        dto.setName("홍길동");
        dto.setFit("standard");
        dto.setHeight(170.0);
        dto.setWeight(60.0);

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호가 너무 깁니다.")));
    }

    @Test
    void Register_72바이트_ASCII_비밀번호는_통과한다() {
        MemberRequestDto.Register dto = new MemberRequestDto.Register();
        dto.setEmail("a@a.com");
        dto.setPassword("a".repeat(72));
        dto.setName("홍길동");
        dto.setFit("standard");
        dto.setHeight(170.0);
        dto.setWeight(60.0);

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);
        assertFalse(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호가 너무 깁니다.")));
    }

    @Test
    void Register_멀티바이트_문자는_글자수가_적어도_바이트기준으로_거부된다() {
        // 한글 1글자 = UTF-8 3바이트. 25자여도 75바이트라 72바이트를 넘긴다 — 글자수 기준
        // @Size(max=72)로는 못 막았을 케이스.
        MemberRequestDto.Register dto = new MemberRequestDto.Register();
        dto.setEmail("a@a.com");
        dto.setPassword("가".repeat(25));
        dto.setName("홍길동");
        dto.setFit("standard");
        dto.setHeight(170.0);
        dto.setWeight(60.0);

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호가 너무 깁니다.")));
    }

    @Test
    void Update_비밀번호가_null이면_바이트길이_검증을_통과한다() {
        MemberRequestDto.Update dto = new MemberRequestDto.Update();
        dto.setPassword(null);

        Set<ConstraintViolation<MemberRequestDto.Update>> violations = validator.validate(dto);
        assertFalse(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호가 너무 깁니다.")));
    }

    @Test
    void Update_73바이트_비밀번호는_거부된다() {
        MemberRequestDto.Update dto = new MemberRequestDto.Update();
        dto.setPassword("a".repeat(73));

        Set<ConstraintViolation<MemberRequestDto.Update>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호가 너무 깁니다.")));
    }
}
