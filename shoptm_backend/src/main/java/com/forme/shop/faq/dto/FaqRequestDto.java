package com.forme.shop.faq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// DTO(Data Transfer Object): 클라이언트 ↔ 서버 간 데이터 전달 전용 객체
public class FaqRequestDto {

    // FAQ 등록 요청 DTO (관리자)
    @Getter @Setter
    public static class Create {

        // Faq.category/question 컬럼 길이(각각 20/200)와 맞춤 — 없으면 DB가 거부하는
        // DataIntegrityViolationException이 그대로 500으로 노출됨(Category DTO와 동일한
        // 이유로 다른 방향에서 버그를 찾다가 발견). answer는 TEXT 컬럼이라 DB 쪽 길이 제한이
        // 없어 여기서는 다루지 않음.
        @NotBlank(message = "카테고리를 선택해주세요.")
        @Size(max = 20, message = "카테고리는 20자 이하여야 합니다.")
        private String category;       // 주문/배송/반품/결제/상품/계정

        @NotBlank(message = "질문을 입력해주세요.")
        @Size(max = 200, message = "질문은 200자 이하여야 합니다.")
        private String question;

        @NotBlank(message = "답변을 입력해주세요.")
        private String answer;

        private Integer sortOrder;     // 정렬 순서 (선택, 기본값 0)
    }

    // FAQ 수정 요청 DTO (관리자)
    // 수정할 항목만 보내면 되므로 선택 입력 — 세 필드 다 null 허용이면서도(안 보내면 그대로
    // 유지) "보냈는데 공백뿐임"은 걸러야 해서 @NotBlank(null 자체를 막음) 대신 @Pattern을 쓴다
    // (MemberRequestDto.Update.name과 동일한 이유·동일한 정규식). category/question은 길이
    // 제한도 같이 걸어야 해서 "비어있지 않음"+"길이 제한"을 하나의 정규식으로 합친다 — 이유는
    // CategoryRequestDto.Update.name과 동일(@Pattern+@Size를 따로 두면 공백만 길게인 입력에서
    // 에러 메시지가 하나로 덮어써짐). 길이 확인에 "\z"를 쓰는 이유도 동일.
    @Getter @Setter
    public static class Update {
        @Pattern(regexp = "(?sU)(?=^.{0,20}\\z).*\\S.*", message = "카테고리를 1~20자로 선택해주세요.")
        private String category;
        @Pattern(regexp = "(?sU)(?=^.{0,200}\\z).*\\S.*", message = "질문을 1~200자로 입력해주세요.")
        private String question;
        @Pattern(regexp = "(?sU).*\\S.*", message = "답변을 입력해주세요.")
        private String answer;
        private Integer sortOrder;
    }
}
