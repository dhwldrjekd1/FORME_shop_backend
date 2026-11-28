package com.forme.shop.member.dto;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.junit.jupiter.api.Assertions.*;

// 지금까지의 테스트는 jakarta.validation.Validator를 직접 호출해 ConstraintViolation의
// propertyPath만 확인했는데, 실제 컨트롤러가 받는 @Valid 검증 실패는 Spring MVC가
// ConstraintViolation을 FieldError로 변환해 GlobalExceptionHandler.handleValidationException()
// 의 ex.getBindingResult().getFieldErrors()로 넘기는 경로를 거친다(교차검증에서 발견 —
// 이 변환 단계 자체를 검증하는 테스트가 없어서, ValidPassword가 필드 레벨 제약이라 위반이
// 이 필드의 경로에 자동으로 잡히는 것이 실제로 FieldError.getField() == "password"로
// 이어지는지는 수동 확인에만 의존하고 있었음). LocalValidatorFactoryBean은 Spring Boot가
// 내부적으로 쓰는 것과 동일한 SpringValidatorAdapter를 쓰므로, 전체 서버 기동 없이도 이
// 변환 단계를 그대로 재현한다.
class MemberRequestDtoPasswordSpringMvcMappingTest {

    private static final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

    static {
        validator.afterPropertiesSet();
    }

    @Test
    void 빈문자열_비밀번호의_FieldError가_실제_필드명_password로_매핑된다() {
        MemberRequestDto.Register dto = new MemberRequestDto.Register();
        dto.setEmail("a@a.com");
        dto.setPassword("");
        dto.setName("홍길동");
        dto.setFit("standard");
        dto.setHeight(170.0);
        dto.setWeight(60.0);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(dto, "register");
        validator.validate(dto, bindingResult);

        FieldError fieldError = bindingResult.getFieldError("password");
        assertNotNull(fieldError, "FieldError가 메서드 이름(예: passwordLengthValid)이 아니라 실제 필드명 password로 나와야 한다");
        assertEquals("비밀번호를 입력해주세요.", fieldError.getDefaultMessage());
    }

    @Test
    void 짧은_비밀번호의_FieldError도_password로_매핑되고_단_하나만_생긴다() {
        MemberRequestDto.Update dto = new MemberRequestDto.Update();
        dto.setPassword("short");

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(dto, "update");
        validator.validate(dto, bindingResult);

        FieldError fieldError = bindingResult.getFieldError("password");
        assertNotNull(fieldError);
        assertEquals("비밀번호는 8자 이상이어야 합니다.", fieldError.getDefaultMessage());
        assertEquals(1, bindingResult.getFieldErrors("password").size());
    }
}
