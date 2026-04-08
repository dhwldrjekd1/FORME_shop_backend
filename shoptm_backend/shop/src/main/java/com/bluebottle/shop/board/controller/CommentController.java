package com.bluebottle.shop.board.controller;

import com.bluebottle.shop.board.dto.CommentRequestDto;
import com.bluebottle.shop.board.dto.CommentResponseDto;
import com.bluebottle.shop.board.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 특정 게시글의 댓글 목록 조회
    // GET /api/boards/{boardId}/comments
    @GetMapping("/boards/{boardId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getComments(
            @PathVariable Long boardId) {
        return ResponseEntity.ok(commentService.getComments(boardId));
    }

    // 내가 작성한 댓글 목록 조회
    // GET /api/members/{memberId}/comments
    @GetMapping("/members/{memberId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getMyComments(
            @PathVariable Long memberId) {
        return ResponseEntity.ok(commentService.getMyComments(memberId));
    }

    // 댓글 작성
    // POST /api/boards/{boardId}/members/{memberId}/comments
    @PostMapping("/boards/{boardId}/members/{memberId}/comments")
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Long boardId,
            @PathVariable Long memberId,
            @Valid @RequestBody CommentRequestDto.Create dto) {
        return ResponseEntity.ok(commentService.createComment(boardId, memberId, dto));
    }

    // 댓글 수정
    // PUT /api/comments/{commentId}
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto.Update dto) {
        return ResponseEntity.ok(commentService.updateComment(commentId, dto));
    }

    // 댓글 삭제 (소프트 삭제)
    // DELETE /api/comments/{commentId}
    // 204 No Content: 처리 성공했지만 반환할 데이터 없음
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    // 관리자 - 댓글 삭제
    // DELETE /api/admin/comments/{commentId}
    @DeleteMapping("/admin/comments/{commentId}")
    public ResponseEntity<Void> adminDeleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}