package com.forme.shop.member.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 비밀번호 필드 하나에 "필수 입력"/"공백뿐 아님"/"8자 이상"/"72바이트 이하"를 각각 별도
// 애노테이션(@NotBlank, @Size 등)으로 걸면, 같은 필드에서 두 개 이상이 동시에 실패하는
// 입력(빈 문자열 등)에서 GlobalExceptionHandler의 필드별 에러 HashMap(키: 필드명)이 한쪽
// 메시지를 덮어써버리는 문제가 반복해서 발견됐다(교차검증에서 발견). 네 가지 조건을 전부
// 이 필드 레벨 제약 하나로 모아 같은 필드에 대해 항상 위반이 최대 1개만 나오도록 한다.
// 처음엔 클래스 레벨(@Target(TYPE))로 짰다가, 그러려면 HasPassword 인터페이스로 Register/
// Update를 묶고 addPropertyNode("password")로 위반 경로를 필드로 되돌려야 하는 등 불필요한
// 장치가 늘어난다는 걸 교차검증에서 지적받아 필드 레벨로 바꿈 — 필드 레벨에서는 위반이 그
// 필드 경로에 자동으로 잡혀 애초에 이런 장치가 필요 없다.
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordConstraintValidator.class)
public @interface ValidPassword {

    // true면 null을 거부(Register), false면 null을 "변경 안 함"으로 통과시킴(Update).
    boolean required();

    // PasswordConstraintValidator가 항상 disableDefaultConstraintViolation() 후 자체 메시지를
    // 만들어서 쓰기 때문에, 이 기본값이 실제 사용자에게 노출될 일은 없다 — Bean Validation의
    // @Constraint 애노테이션은 message() 속성이 필수라 형식상 채워두는 값.
    String message() default "유효하지 않은 비밀번호입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
