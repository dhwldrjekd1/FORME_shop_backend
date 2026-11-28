package com.forme.shop.member.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

// ValidPassword 참고 — Register/Update의 password 검증을 전부 여기 한 곳에 모아 그 필드에
// 최대 1개의 위반만 보고한다. 필드 레벨 제약이라 위반은 자동으로 이 필드의 경로("password")에
// 잡히므로, 경로를 수동으로 되돌리는 처리가 필요 없다.
public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_PASSWORD_LENGTH = 8;

    // BCrypt가 encode() 단계에서 이 바이트 수를 넘으면 예외를 던지는 상한.
    private static final int MAX_PASSWORD_BYTES = 72;

    // "공백뿐인 문자가 하나라도 있는가"를 판정하는 기준. String.trim()/isBlank()는 ASCII
    // 공백만 지우고 전각 공백(U+3000, 한국어 IME에서 흔함)·줄바꿈 없는 공백(U+00A0,
    // 복사-붙여넣기에서 흔함)으로만 된 값을 걸러내지 못해서(교차검증에서 발견), name/address
    // 필드에 이미 쓰고 있는 (?sU)...\S.* 정규식과 동일한 기준(UNICODE_CHARACTER_CLASS)을 쓴다.
    private static final Pattern HAS_NON_BLANK_CHAR = Pattern.compile("(?sU).*\\S.*");

    private boolean required;

    @Override
    public void initialize(ValidPassword annotation) {
        this.required = annotation.required();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        String message = failureMessage(password);
        if (message == null) return true;

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }

    // 실패하지 않으면 null, 실패하면 보여줄 메시지를 반환한다. 우선순위: 입력 자체가 없음/
    // 공백뿐임 → 너무 짧음 → 너무 길음. 한 번에 하나씩만 보고해서 ValidPassword의 목적(필드
    // 하나당 위반 최대 1개)을 지킨다.
    private String failureMessage(String password) {
        if (password == null) {
            return required ? "비밀번호를 입력해주세요." : null;   // Update는 null = 변경 안 함
        }
        if (!HAS_NON_BLANK_CHAR.matcher(password).matches()) {
            return "비밀번호를 입력해주세요.";
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "비밀번호는 8자 이상이어야 합니다.";
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return "비밀번호가 너무 깁니다.";
        }
        return null;
    }
}
