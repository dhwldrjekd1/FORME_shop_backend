package com.forme.shop.board.dto;

import com.forme.shop.board.entity.Board;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

// 클라이언트에게 게시글 정보를 응답할 때 사용하는 DTO
@Getter
@Builder
public class BoardResponseDto {

    private Long id;
    private Long memberId;
    private String memberName;     // 작성자 이름
    private String title;          // 게시글 제목
    private String content;        // 게시글 내용
    private Integer views;         // 조회수
    private Integer totalRevenue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Board 엔티티를 BoardResponseDto 로 변환하는 정적 메서드
    public static BoardResponseDto from(Board board) {
        return BoardResponseDto.builder()
                .id(board.getId())
                .memberId(board.getMember().getId())
                .memberName(board.getMember().getName())
                .title(board.getTitle())
                .content(board.getContent())
                .views(board.getViews())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }
}