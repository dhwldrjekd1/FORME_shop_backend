package com.forme.shop.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class DeliveryRequestDto {

    // 배송 정보 등록 요청 DTO (관리자)
    @Getter @Setter
    public static class Create {

        // Delivery.carrier/trackingNumber 컬럼 길이(각각 50)와 맞춤 — 없으면 DB가 거부하는
        // DataIntegrityViolationException이 그대로 500으로 노출됨(Category/FAQ/Board/Qna/Member/
        // Settings DTO에서 이미 고쳤던 것과 같은 종류인데 배송 쪽만 빠져있었음 — 다른 방향에서
        // 버그를 찾다가 발견). 관리자 전용 입력이라 심각도는 낮지만(오타·붙여넣기 실수 정도),
        // 같은 종류의 500 노출은 막는 게 맞음.
        @NotBlank(message = "택배사를 입력해주세요.")
        @Size(max = 50, message = "택배사명은 50자 이하여야 합니다.")
        private String carrier;        // 택배사명

        @Size(max = 50, message = "운송장 번호는 50자 이하여야 합니다.")
        private String trackingNumber; // 운송장 번호 (선택)
    }

    // 배송 상태 수정 요청 DTO (관리자)
    @Getter @Setter
    public static class Update {

        // 길이 제한도 같이 걸어야 해서 "비어있지 않음"+"길이 제한"을 하나의 정규식으로 합친다 —
        // 이유는 CategoryRequestDto.Update.name과 동일(@Pattern과 @Size를 따로 두면 공백만
        // 51자처럼 둘 다 동시에 실패하는 입력에서 GlobalExceptionHandler가 필드별 에러를
        // HashMap에 담다가 한쪽 메시지를 덮어씀). 길이 확인에 "\z"를 쓰는 이유도 동일.
        @Pattern(regexp = "(?sU)(?=^.{0,50}\\z).*\\S.*", message = "택배사를 1~50자로 입력해주세요.")
        private String carrier;        // 택배사명 (선택)

        // trackingNumber는 Create에서도 필수가 아니라 빈 문자열로 등록될 수 있고(관리자가
        // 운송장 번호 없이 배송을 먼저 등록한 뒤 상태만 갱신하는 흐름), AdminOrders.vue의
        // updateDelivery가 현재 상태 전체를 매번 그대로 다시 보내므로, 여기에 공백 거부를
        // 걸면 상태만 바꾸려는 요청까지 막힌다. carrier와 달리 이 필드는 애초에 "비어있는 게
        // 정상"인 값이라 공백 거부를 적용하지 않는다. 길이 제한(Delivery.trackingNumber
        // 컬럼 길이 50)만 별도로 건다 — @Size는 null/빈 문자열을 그대로 통과시키므로 이
        // "비어있는 게 정상" 특성과 충돌하지 않는다.
        @Size(max = 50, message = "운송장 번호는 50자 이하여야 합니다.")
        private String trackingNumber; // 운송장 번호 (선택)

        // 네 가지 값 중 하나가 아니면(오타 등) 그 배송 건이 상태별 목록 조회
        // (DeliveryService.getDeliveriesByStatus)에서 어디에도 걸리지 않고 조용히
        // 빠져버릴 수 있어 화이트리스트로 막는다.
        @NotBlank(message = "배송 상태를 입력해주세요.")
        @Pattern(regexp = "READY|IN_TRANSIT|OUT_FOR_DELIVERY|DELIVERED", message = "배송 상태 값이 올바르지 않습니다.")
        private String status;
        // READY / IN_TRANSIT / OUT_FOR_DELIVERY / DELIVERED

        // ProductService.updateProduct()의 expectedUpdatedAt과 같은 목적. AdminOrders.vue가
        // updateDelivery 호출마다 현재 화면에 보이는 상태 전체를 그대로 다시 보내는 구조라,
        // 관리자가 이 배송 건의 관리 화면을 열어둔 사이(또는 다른 관리자가 동시에) 상태가 먼저
        // 바뀌면, 그 변경을 못 보고 화면을 연 시점의 오래된 상태로 덮어쓸 수 있다(이중클릭,
        // 또는 서로 다른 관리자의 동시 처리 — 교차검증에서 발견). null이면(구버전 클라이언트 등)
        // 이 확인을 건너뛴다.
        private LocalDateTime expectedUpdatedAt;
    }
}