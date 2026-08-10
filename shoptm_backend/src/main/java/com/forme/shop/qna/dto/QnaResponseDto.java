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

    // Qna 엔티티를 QnaResponseDto로 변환하는 정적 메서드 (내용을 가리지 않고 그대로 노출)
    // 작성자 본인이 방금 쓰거나 수정한 직후, 혹은 관리자 전용 경로처럼 호출한 쪽이 이미
    // 소유자/관리자임이 확인된 경우에만 사용해야 함 — 그 외 목록/단건 조회는 아래
    // from(qna, canViewSecret) 오버로드로 비밀글 내용을 가려야 함
    public static QnaResponseDto from(Qna qna) {
        return from(qna, true);
    }

    // canViewSecret=false면, 비밀글(isSecret=true)의 content/answer는 null로 가려서 내려준다.
    // (title/isSecret/작성자 등 목록에 필요한 메타 정보는 그대로 노출 — 잠금 아이콘 표시용)
    public static QnaResponseDto from(Qna qna, boolean canViewSecret) {
        boolean hide = Boolean.TRUE.equals(qna.getIsSecret()) && !canViewSecret;
        return QnaResponseDto.builder()
                .id(qna.getId())
                .memberId(qna.getMember().getId())
                .memberName(qna.getMember().getName())
                // 상품이 없을 수도 있으므로 null 체크
                .productId(qna.getProduct() != null ? qna.getProduct().getId() : null)
                .productName(qna.getProduct() != null ? qna.getProduct().getName() : null)
                .title(qna.getTitle())
                .content(hide ? null : qna.getContent())
                .answer(hide ? null : qna.getAnswer())
                .isSecret(qna.getIsSecret())
                .isAnswered(qna.getAnswer() != null)  // 답변 있으면 true
                .answeredAt(qna.getAnsweredAt())
                .createdAt(qna.getCreatedAt())
                .updatedAt(qna.getUpdatedAt())
                .build();
    }
}