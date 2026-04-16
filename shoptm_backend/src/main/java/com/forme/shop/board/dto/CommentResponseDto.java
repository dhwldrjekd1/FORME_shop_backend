package com.forme.shop.board.dto;

import com.forme.shop.board.entity.Comment;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

// 클라이언트에게 댓글 정보를 응답할 때 사용하는 DTO
@Getter
@Builder
public class CommentResponseDto {

    private Long id;
    private Long boardId;          // 게시글 ID
    private Long memberId;         // 작성자 ID
    private String memberName;     // 작성자 이름
    private String content;        // 댓글 내용
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Comment 엔티티를 CommentResponseDto 로 변환하는 정적 메서드
    public static CommentResponseDto from(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .boardId(comment.getBoard().getId())
                .memberId(comment.getMember().getId())
                .memberName(comment.getMember().getName())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}