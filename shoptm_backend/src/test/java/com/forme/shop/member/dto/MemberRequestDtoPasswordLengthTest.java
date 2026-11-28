package com.forme.shop.member.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// 비밀번호 검증을 ValidPassword(클래스 레벨 커스텀 제약, PasswordConstraintValidator)로
// 통합한 뒤 확인. 원래 Register.password는 @NotBlank와 @Size(min=8)를 따로 둬서 빈 문자열
// ("")처럼 둘 다 동시에 실패하는 입력에서 GlobalExceptionHandler의 필드별 에러 HashMap(키:
// 필드명)이 한쪽 메시지를 덮어쓸 수 있었고(교차검증에서 발견), Update.password는 애초에
// 공백뿐인 값을 막는 검증이 없어서 공백 8자가 그대로 통과해 실제 비밀번호로 저장될 수
// 있었음(다른 방향에서 버그를 찾다가 발견). 이 테스트는 그 두 가지 모두와, 위반이 항상
// 실제 필드명("password") 하나에만 보고되는지를 함께 검증한다.
class MemberRequestDtoPasswordLengthTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private MemberRequestDto.Register validRegisterDto() {
        MemberRequestDto.Register dto = new MemberRequestDto.Register();
        dto.setEmail("a@a.com");
        dto.setName("홍길동");
        dto.setFit("standard");
        dto.setHeight(170.0);
        dto.setWeight(60.0);
        return dto;
    }

    private <T> String violationMessageOnPasswordPath(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("password"))
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(null);
    }

    @Test
    void 빈문자열_비밀번호는_password_필드_하나에만_위반이_보고된다() {
        MemberRequestDto.Register dto = validRegisterDto();
        dto.setPassword("");

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);
        long onPasswordPath = violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("password"))
                .count();
        assertEquals(1, onPasswordPath);
        assertEquals("비밀번호를 입력해주세요.", violationMessageOnPasswordPath(violations));
    }

    @Test
    void 공백8자_비밀번호는_공백뿐이라_거부된다() {
        MemberRequestDto.Register dto = validRegisterDto();
        dto.setPassword(" ".repeat(8));

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);
        assertEquals("비밀번호를 입력해주세요.", violationMessageOnPasswordPath(violations));
    }

    @Test
    void 짧은_7자_비밀번호는_8자이상_메시지로_거부된다() {
        MemberRequestDto.Register dto = validRegisterDto();
        dto.setPassword("abcdefg");

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);
        assertEquals("비밀번호는 8자 이상이어야 합니다.", violationMessageOnPasswordPath(violations));
    }

    @Test
    void 정확히_8자_비밀번호는_통과한다() {
        MemberRequestDto.Register dto = validRegisterDto();
        dto.setPassword("abcdefgh");

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);
        assertTrue(violations.stream().noneMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void null_비밀번호는_필수입력_메시지로_거부된다() {
        MemberRequestDto.Register dto = validRegisterDto();
        dto.setPassword(null);

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);
        assertEquals("비밀번호를 입력해주세요.", violationMessageOnPasswordPath(violations));
    }

    @Test
    void 전각공백_8자_비밀번호는_String_trim으로는_안_걸러지지만_거부된다() {
        // U+3000(전각 공백)은 String.trim()/isBlank()로는 공백으로 인식되지 않는 경우가
        // 있어(한국어 IME에서 흔함), trim() 기반 검사로 짰다면 이 케이스를 놓쳤을 것 —
        // name/address에 이미 쓰고 있는 (?sU)\S 정규식 기준으로 통일해 막는다.
        MemberRequestDto.Register dto = validRegisterDto();
        dto.setPassword("　".repeat(8));

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);
        assertEquals("비밀번호를 입력해주세요.", violationMessageOnPasswordPath(violations));
    }

    @Test
    void Update_공백뿐인_8자_비밀번호는_거부된다() {
        // 예전엔 Update.password에 @Size(min=8)만 있어서 공백뿐인 8자가 그대로 통과해 실제
        // 비밀번호로 저장될 수 있었음(다른 방향에서 버그를 찾다가 발견) — Register와 동일한
        // 검증으로 맞춘 뒤 재확인.
        MemberRequestDto.Update dto = new MemberRequestDto.Update();
        dto.setPassword(" ".repeat(8));

        Set<ConstraintViolation<MemberRequestDto.Update>> violations = validator.validate(dto);
        assertEquals("비밀번호를 입력해주세요.", violationMessageOnPasswordPath(violations));
    }

    @Test
    void Update_비밀번호가_null이면_통과한다() {
        // Update는 선택 입력 — null은 "비밀번호를 바꾸지 않음"을 뜻하므로 통과해야 한다.
        MemberRequestDto.Update dto = new MemberRequestDto.Update();
        dto.setPassword(null);

        Set<ConstraintViolation<MemberRequestDto.Update>> violations = validator.validate(dto);
        assertTrue(violations.stream().noneMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void Update_8자_비밀번호는_통과한다() {
        MemberRequestDto.Update dto = new MemberRequestDto.Update();
        dto.setPassword("abcdefgh");

        Set<ConstraintViolation<MemberRequestDto.Update>> violations = validator.validate(dto);
        assertTrue(violations.stream().noneMatch(v -> v.getPropertyPath().toString().equals("password")));
    }
}
