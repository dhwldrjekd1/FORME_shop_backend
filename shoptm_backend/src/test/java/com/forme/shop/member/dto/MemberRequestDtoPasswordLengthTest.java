package com.forme.shop.member.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// Register.password가 원래 @NotBlank와 @Size(min=8)를 따로 두고 있어서, 빈 문자열("")처럼
// 둘 다 동시에 실패하는 입력에서 GlobalExceptionHandler의 필드별 에러 HashMap(키: 필드명)이
// 한쪽 메시지를 덮어쓸 수 있던 문제(교차검증에서 발견)를 isPasswordLengthValid()로 합친 뒤
// 검증. 서로 다른 property path(password / passwordLengthValid)로 분리돼 더는 같은 키를
// 공유하지 않는지까지 직접 확인한다.
class MemberRequestDtoPasswordLengthTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private MemberRequestDto.Register validDto() {
        MemberRequestDto.Register dto = new MemberRequestDto.Register();
        dto.setEmail("a@a.com");
        dto.setName("홍길동");
        dto.setFit("standard");
        dto.setHeight(170.0);
        dto.setWeight(60.0);
        return dto;
    }

    @Test
    void 빈문자열_비밀번호는_NotBlank와_길이검증_메시지가_서로_다른_키로_둘다_살아있다() {
        MemberRequestDto.Register dto = validDto();
        dto.setPassword("");

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);

        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호를 입력해주세요.")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호는 8자 이상이어야 합니다.")));
        // 예전 구조(@NotBlank + @Size, 둘 다 "password" 키)였다면 HashMap에서 한쪽이
        // 사라졌을 것 — 지금은 서로 다른 property path를 쓰므로 둘 다 살아남는다.
        long distinctPaths = violations.stream().map(v -> v.getPropertyPath().toString()).distinct().count();
        assertEquals(violations.size(), distinctPaths);
    }

    @Test
    void 공백8자_비밀번호는_공백뿐이라_거부된다() {
        MemberRequestDto.Register dto = validDto();
        dto.setPassword(" ".repeat(8));

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호는 8자 이상이어야 합니다.")));
    }

    @Test
    void 짧은_7자_비밀번호는_길이검증에서만_거부된다() {
        MemberRequestDto.Register dto = validDto();
        dto.setPassword("abcdefg");

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);
        assertFalse(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호를 입력해주세요.")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호는 8자 이상이어야 합니다.")));
    }

    @Test
    void 정확히_8자_비밀번호는_통과한다() {
        MemberRequestDto.Register dto = validDto();
        dto.setPassword("abcdefgh");

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);
        assertFalse(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호를 입력해주세요.")));
        assertFalse(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호는 8자 이상이어야 합니다.")));
    }

    @Test
    void null_비밀번호는_NotBlank에서만_거부되고_길이검증은_통과한다() {
        MemberRequestDto.Register dto = validDto();
        dto.setPassword(null);

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호를 입력해주세요.")));
        assertFalse(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호는 8자 이상이어야 합니다.")));
    }

    @Test
    void 전각공백_8자_비밀번호는_String_trim으로는_안_걸러지지만_거부된다() {
        // U+3000(전각 공백)은 String.trim()/isBlank()로는 공백으로 인식되지 않는 경우가
        // 있어(한국어 IME에서 흔함), trim() 기반 검사로 짰다면 이 케이스를 놓쳤을 것 —
        // name/address에 이미 쓰고 있는 (?sU)\S 정규식 기준으로 통일해 막는다.
        MemberRequestDto.Register dto = validDto();
        dto.setPassword("　".repeat(8));

        Set<ConstraintViolation<MemberRequestDto.Register>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호는 8자 이상이어야 합니다.")));
    }

    @Test
    void Update_공백뿐인_8자_비밀번호는_거부된다() {
        // 예전엔 Update.password에 @Size(min=8)만 있어서 공백뿐인 8자가 그대로 통과해 실제
        // 비밀번호로 저장될 수 있었음(다른 방향에서 버그를 찾다가 발견) — Register와 동일한
        // 검증으로 맞춘 뒤 재확인.
        MemberRequestDto.Update dto = new MemberRequestDto.Update();
        dto.setPassword(" ".repeat(8));

        Set<ConstraintViolation<MemberRequestDto.Update>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호는 8자 이상이어야 합니다.")));
    }

    @Test
    void Update_비밀번호가_null이면_길이검증을_통과한다() {
        MemberRequestDto.Update dto = new MemberRequestDto.Update();
        dto.setPassword(null);

        Set<ConstraintViolation<MemberRequestDto.Update>> violations = validator.validate(dto);
        assertFalse(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호는 8자 이상이어야 합니다.")));
    }

    @Test
    void Update_8자_비밀번호는_통과한다() {
        MemberRequestDto.Update dto = new MemberRequestDto.Update();
        dto.setPassword("abcdefgh");

        Set<ConstraintViolation<MemberRequestDto.Update>> violations = validator.validate(dto);
        assertFalse(violations.stream().anyMatch(v -> v.getMessage().equals("비밀번호는 8자 이상이어야 합니다.")));
    }
}
