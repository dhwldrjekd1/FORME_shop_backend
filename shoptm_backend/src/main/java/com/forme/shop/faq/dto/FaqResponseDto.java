package com.forme.shop.faq.dto;

import com.forme.shop.faq.entity.Faq;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 클라이언트에게 FAQ 정보를 응답할 때 사용하는 DTO
@Getter
@Builder
public class FaqResponseDto {

    private Long id;
    private String category;
    private String question;
    private String answer;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Faq 엔티티를 FaqResponseDto로 변환하는 정적 메서드
    public static FaqResponseDto from(Faq faq) {
        return FaqResponseDto.builder()
                .id(faq.getId())
                .category(faq.getCategory())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .sortOrder(faq.getSortOrder())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}
