package com.bluebottle.shop.delivery.dto;

import jakarta.validation.constraints.NotBlank;
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

        private String carrier;        // 택배사명 (선택)
        private String trackingNumber; // 운송장 번호 (선택)

        @NotBlank(message = "배송 상태를 입력해주세요.")
        private String status;
        // READY / IN_TRANSIT / OUT_FOR_DELIVERY / DELIVERED
    }
}