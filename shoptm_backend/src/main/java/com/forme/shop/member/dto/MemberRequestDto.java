package com.forme.shop.member.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

        // 주석엔 "필수"라고 돼 있었지만 정작 검증 애노테이션이 없어서, 프론트(SignupView.vue)는
        // 이미 키 100~250cm·몸무게 30~200kg으로 막고 있는데도 API를 직접 호출하면 이 범위를
        // 벗어나거나 아예 없는 값으로도 가입이 그대로 통과했음. 프론트의 검증 범위와 동일하게 맞춤.
        @NotNull(message = "키를 입력해주세요.")
        @DecimalMin(value = "100", message = "키는 100cm 이상이어야 합니다.")
        @DecimalMax(value = "250", message = "키는 250cm 이하여야 합니다.")
        private Double height;     // 키 (cm) - 필수

        @NotNull(message = "몸무게를 입력해주세요.")
        @DecimalMin(value = "30", message = "몸무게는 30kg 이상이어야 합니다.")
        @DecimalMax(value = "200", message = "몸무게는 200kg 이하여야 합니다.")
        private Double weight;     // 몸무게 (kg) - 필수

        @NotBlank(message = "선호 핏을 선택해주세요.")
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

        // phone은 Register(가입)에서도 애초에 필수가 아니라(@NotBlank 없음) 실제로 빈 값으로
        // 남아있는 계정이 있을 수 있다 — 마이페이지(MyPageView.vue)가 저장할 때마다 폼의 현재
        // phone 값을 그대로 다시 보내므로(전화번호를 등록한 적 없으면 빈 문자열), 여기에 공백
        // 거부를 걸면 그런 회원이 이름 등 다른 항목만 바꾸려 해도 요청 전체가 실패한다.
        private String phone;

        // address는 phone과 달리 마이페이지 저장 요청에 아예 포함되지 않는(그 화면에 입력
        // UI 자체가 없음) 필드라 이 문제가 없다 — name과 동일하게 "보냈는데 공백뿐임"만 거른다.
        @Pattern(regexp = "(?sU).*\\S.*", message = "주소를 입력해주세요.")
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

        // fit은 Register에서도 실제로는 필수 검증이 없지만(주석만 "필수"), 마이페이지에서는
        // 항상 고정된 값(slim/standard/wide) 중 하나를 고르는 버튼으로만 바뀌고 초기값도
        // "standard"로 채워져 있어(MyPageView.vue) 공백으로 전송될 일이 없다 — phone과 달리
        // 실제로 빈 문자열이 저장돼 있을 걱정이 없으므로 공백 거부를 적용한다.
        @Pattern(regexp = "(?sU).*\\S.*", message = "핏을 선택해주세요.")
        private String fit;
    }

    // 회원탈퇴 요청 시 받을 데이터. 본인이 자기 계정을 탈퇴할 때만 currentPassword가 필요하고
    // (MemberService.withdraw() 참고), 관리자가 다른 회원을 대신 탈퇴 처리할 땐 필요 없어서
    // 필수(@NotBlank)로 두지 않는다 — 대신 서비스 계층에서 본인 여부에 따라 조건부로 확인한다.
    @Getter @Setter
    public static class Withdraw {
        private String currentPassword;
    }

    // 관리자 - 회원 강퇴 요청 시 받을 데이터. 관리자가 자기 자신의 계정을 대상으로 강퇴를
    // 호출할 때만 currentPassword가 필요하고(MemberService.banMember() 참고), 다른 회원을
    // 강퇴할 땐 필요 없어서 필수(@NotBlank)로 두지 않는다.
    @Getter @Setter
    public static class Ban {
        private String currentPassword;
    }
}