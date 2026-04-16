package com.forme.shop.qna.dto;

import com.forme.shop.qna.entity.Qna;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 클라이언트에게 Q&A 정보를 응답할 때 사용하는 DTO
@Getter
@Builder
public class QnaResponseDto {

    private Long id;
    private Long memberId;
    private String memberName;     // 질문 작성자 이름
    private Long productId;
    private String productName;    // 질문 대상 상품명
    private String title;          // 질문 제목
    private String content;        // 질문 내용
    private String answer;         // 관리자 답변 (null = 미답변)
    private Boolean isSecret;      // 비밀글 여부
    private Boolean isAnswered;    // 답변 여부 (answer != null)
    private LocalDateTime answeredAt;   // 답변 등록 시간
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Qna 엔티티를 QnaResponseDto로 변환하는 정적 메서드
    public static QnaResponseDto from(Qna qna) {
        return QnaResponseDto.builder()
                .id(qna.getId())
                .memberId(qna.getMember().getId())
                .memberName(qna.getMember().getName())
                // 상품이 없을 수도 있으므로 null 체크
                .productId(qna.getProduct() != null ? qna.getProduct().getId() : null)
                .productName(qna.getProduct() != null ? qna.getProduct().getName() : null)
                .title(qna.getTitle())
                .content(qna.getContent())
                .answer(qna.getAnswer())
                .isSecret(qna.getIsSecret())
                .isAnswered(qna.getAnswer() != null)  // 답변 있으면 true
                .answeredAt(qna.getAnsweredAt())
                .createdAt(qna.getCreatedAt())
                .updatedAt(qna.getUpdatedAt())
                .build();
    }
}