package com.forme.shop.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// DTO(Data Transfer Object): 클라이언트 ↔ 서버 간 데이터 전달 전용 객체
// Entity를 직접 노출하지 않고 DTO를 사용해 필요한 데이터만 주고받음
public class MemberRequestDto {

    // 회원가입 요청 시 받을 데이터
    @Getter @Setter
    public static class Register {

        @Email(message = "이메일 형식이 아닙니다.")       // 이메일 형식 검증
        @NotBlank(message = "이메일을 입력해주세요.")      // null, 빈 문자열 불가
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")  // 최소 8자 검증
        private String password;

        @NotBlank(message = "이름을 입력해주세요.")
        private String name;
        private String phone;
        private String address;
        private Double height;     // 키 (cm) - 필수
        private Double weight;     // 몸무게 (kg) - 필수
        private String fit;        // 선호 핏 (slim, standard, wide) - 필수
    }

    // 로그인 요청 시 받을 데이터
    @Getter @Setter
    public static class Login {

        @NotBlank(message = "이메일을 입력해주세요.")
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        private String password;
    }

    // 회원정보 수정 요청 시 받을 데이터
    // 수정할 항목만 보내면 되므로 전부 선택 입력 (null 허용)
    @Getter @Setter
    public static class Update {
        private String name;
        private String phone;
        private String address;
        private String password;
        private Double height;
        private Double weight;
        private String fit;
    }
}