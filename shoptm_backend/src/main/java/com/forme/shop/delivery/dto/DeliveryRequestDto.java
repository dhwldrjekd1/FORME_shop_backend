package com.forme.shop.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

public class DeliveryRequestDto {

    // 배송 정보 등록 요청 DTO (관리자)
    @Getter @Setter
    public static class Create {

        @NotBlank(message = "택배사를 입력해주세요.")
        private String carrier;        // 택배사명

        private String trackingNumber; // 운송장 번호 (선택)
    }

    // 배송 상태 수정 요청 DTO (관리자)
    @Getter @Setter
    public static class Update {

        @Pattern(regexp = "(?sU).*\\S.*", message = "택배사를 입력해주세요.")
        private String carrier;        // 택배사명 (선택)

        // trackingNumber는 Create에서도 필수가 아니라 빈 문자열로 등록될 수 있고(관리자가
        // 운송장 번호 없이 배송을 먼저 등록한 뒤 상태만 갱신하는 흐름), AdminOrders.vue의
        // updateDelivery가 현재 상태 전체를 매번 그대로 다시 보내므로, 여기에 공백 거부를
        // 걸면 상태만 바꾸려는 요청까지 막힌다. carrier와 달리 이 필드는 애초에 "비어있는 게
        // 정상"인 값이라 공백 거부를 적용하지 않는다.
        private String trackingNumber; // 운송장 번호 (선택)

        @NotBlank(message = "배송 상태를 입력해주세요.")
        private String status;
        // READY / IN_TRANSIT / OUT_FOR_DELIVERY / DELIVERED
    }
}