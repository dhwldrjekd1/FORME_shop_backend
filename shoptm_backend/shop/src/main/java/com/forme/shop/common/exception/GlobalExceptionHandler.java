package com.forme.shop.common.exception;

import com.forme.shop.common.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// @RestControllerAdvice: 모든 컨트롤러에서 발생하는 예외를 한 곳에서 처리
// 전역 예외 처리기
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid 유효성 검증 실패 시 발생하는 예외 처리
    // 예: @NotBlank, @Email, @Size 등 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(
            MethodArgumentNotValidException ex) {

        // 필드별 에러 메시지 수집
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            // key: 필드명, value: 에러 메시지
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)  // 400
                .body(ErrorResponseDto.builder()
                        .status(400)
                        .message("입력값이 올바르지 않습니다.")
                        .errors(errors)           // 필드별 에러 메시지
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    // 비즈니스 로직 예외 처리
    // 예: 이메일 중복, 재고 부족, 존재하지 않는 회원 등
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)  // 400
                .body(ErrorResponseDto.builder()
                        .status(400)
                        .message(ex.getMessage())  // 서비스에서 던진 메시지 그대로 반환
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    // 인증되지 않은 사용자 예외 처리
    // 예: 로그인 없이 보호된 API 접근
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponseDto> handleSecurityException(
            SecurityException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)  // 401
                .body(ErrorResponseDto.builder()
                        .status(401)
                        .message("로그인이 필요합니다.")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    // 그 외 예상치 못한 예외 처리
    // 500 Internal Server Error 반환
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(Exception ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)  // 500
                .body(ErrorResponseDto.builder()
                        .status(500)
                        .message("서버 오류가 발생했습니다.")
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}