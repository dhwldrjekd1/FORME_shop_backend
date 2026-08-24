package com.forme.shop.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    // 수정할 항목만 보내면 되므로 전부 선택 입력 (null 허용) — @NotBlank는 null 자체를 막아버려
    // 이 선택 입력 방식과 안 맞으므로 쓸 수 없고, @Size/@Pattern은 null은 통과시키고 "보냈는데
    // 비어있음/공백뿐임/너무 짧음"만 걸러내므로 이 방식과 호환된다.
    @Getter @Setter
    public static class Update {
        // 공백만 있는 이름(" " 등)도 걸러내야 해서 @Size(min=1) 대신 "공백이 아닌 문자가 하나라도
        // 있어야 함"을 뜻하는 정규식을 씀 — Register.name의 @NotBlank(trim 후 빈 문자열 판정)와
        // 동일한 기준을 null 허용 필드에서도 유지하기 위함.
        // 플래그: s(DOTALL) 없으면 개행이 포함된 이름이 매치 실패하고, U(UNICODE_CHARACTER_CLASS)
        // 없으면 \S가 ASCII 공백만 인식해 전각 공백(U+3000, 한국어 IME에서 흔함)이나 줄바꿈
        // 없는 공백(U+00A0, 복사-붙여넣기에서 흔함)만으로 된 이름을 걸러내지 못한다.
        @Pattern(regexp = "(?sU).*\\S.*", message = "이름을 입력해주세요.")
        private String name;
        private String phone;
        private String address;

        // 가입 시(Register.password)와 동일한 최소 길이를 요구한다. 예전엔 여기에 검증이
        // 전혀 없어서 빈 문자열/한 글자짜리 비밀번호로도 그대로 변경될 수 있었음.
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        private String password;

        // 본인이 자기 비밀번호를 바꿀 때만 필요(MemberService.update() 참고) — 세션이 탈취돼도
        // 현재 비밀번호를 모르면 새 비밀번호로 덮어쓸 수 없도록 막기 위함. 관리자가 다른 회원의
        // 비밀번호를 대신 바꿔줄 땐 필요 없음.
        private String currentPassword;

        private Double height;
        private Double weight;
        private String fit;
    }

    // 회원탈퇴 요청 시 받을 데이터. 본인이 자기 계정을 탈퇴할 때만 currentPassword가 필요하고
    // (MemberService.withdraw() 참고), 관리자가 다른 회원을 대신 탈퇴 처리할 땐 필요 없어서
    // 필수(@NotBlank)로 두지 않는다 — 대신 서비스 계층에서 본인 여부에 따라 조건부로 확인한다.
    @Getter @Setter
    public static class Withdraw {
        private String currentPassword;
    }
}