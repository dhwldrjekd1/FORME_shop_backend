package com.forme.shop.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class CategoryRequestDto {

    // 카테고리 등록 요청 DTO (관리자)
    @Getter @Setter
    public static class Create {

        // Category.name/description 컬럼 길이(각각 50/200)와 맞춤 — 없으면 DB가 거부하는
        // DataIntegrityViolationException이 그대로 500으로 노출됨(주문/게시판/QnA/회원 DTO에서
        // 고쳤던 것과 같은 종류인데 카테고리 쪽만 빠져있었음 — 다른 방향에서 버그를 찾다가 발견).
        // 관리자 전용 입력이라 심각도는 낮지만(오타·붙여넣기 실수 정도), 같은 종류의 500 노출은
        // 막는 게 맞음.
        @NotBlank(message = "카테고리명을 입력해주세요.")
        @Size(max = 50, message = "카테고리명은 50자 이하여야 합니다.")
        private String name;           // 카테고리명

        @Size(max = 200, message = "설명은 200자 이하여야 합니다.")
        private String description;    // 설명 (선택)

        private Integer sortOrder = 0; // 정렬 순서 (기본값 0)
    }

    // 카테고리 수정 요청 DTO (관리자)
    // 수정할 항목만 보내면 되므로 선택 입력 — name은 null 허용이면서도(안 보내면 그대로 유지)
    // "보냈는데 공백뿐임"은 걸러야 해서 @NotBlank(null 자체를 막음) 대신 @Pattern을 쓴다
    // (MemberRequestDto.Update.name과 동일한 이유·동일한 정규식). 길이 제한도 같이 걸어야 해서
    // "비어있지 않음"+"길이 제한"을 하나의 정규식으로 합친다 — @Pattern과 @Size를 따로 두면
    // 공백만 51자처럼 둘 다 동시에 실패하는 입력에서 GlobalExceptionHandler가 필드별 에러를
    // HashMap에 담다가 한쪽 메시지를 덮어쓰는 문제가 있어서다(Board/Qna/Member DTO와 동일한
    // 이유). 길이 확인에 "$" 대신 "\z"를 쓰는 이유도 그때와 동일("$"는 끝의 줄바꿈 앞에서도
    // 매칭돼 길이 제한이 우회될 수 있었음).
    @Getter @Setter
    public static class Update {
        @Pattern(regexp = "(?sU)(?=^.{0,50}\\z).*\\S.*", message = "카테고리명을 1~50자로 입력해주세요.")
        private String name;

        @Size(max = 200, message = "설명은 200자 이하여야 합니다.")
        private String description;

        private Integer sortOrder;
        private Boolean isActive;      // 활성/비활성 변경
    }
}