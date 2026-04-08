package com.bluebottle.shop.common.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

// 에러 응답 DTO
// 클라이언트에게 에러 정보를 일관된 형식으로 반환
@Getter
@Builder
public class ErrorResponseDto {

    private int status;                     // HTTP 상태 코드 (400, 401, 403, 404 등)
    private String message;                 // 에러 메시지
    private Map<String, String> errors;     // 필드별 유효성 검증 에러 (폼 검증 실패 시)
    private LocalDateTime timestamp;        // 에러 발생 시간
}