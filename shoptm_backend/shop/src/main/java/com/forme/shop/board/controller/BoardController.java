package com.forme.shop.board.controller;

import com.forme.shop.board.dto.BoardRequestDto;
import com.forme.shop.board.dto.BoardResponseDto;
import com.forme.shop.board.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // 전체 게시글 목록 조회
    // GET /api/boards
    @GetMapping("/boards")
    public ResponseEntity<List<BoardResponseDto>> getAllBoards() {
        return ResponseEntity.ok(boardService.getAllBoards());
    }

    // 게시글 단건 조회 + 조회수 증가
    // GET /api/boards/{boardId}
    @GetMapping("/boards/{boardId}")
    public ResponseEntity<BoardResponseDto> getBoard(
            @PathVariable Long boardId) {
        return ResponseEntity.ok(boardService.getBoard(boardId));
    }

    // 내가 작성한 게시글 목록 조회
    // GET /api/members/{memberId}/boards
    @GetMapping("/members/{memberId}/boards")
    public ResponseEntity<List<BoardResponseDto>> getMyBoards(
            @PathVariable Long memberId) {
        return ResponseEntity.ok(boardService.getMyBoards(memberId));
    }

    // 게시글 검색
    // GET /api/boards/search?keyword=원두
    @GetMapping("/boards/search")
    public ResponseEntity<List<BoardResponseDto>> searchBoards(
            @RequestParam String keyword) {
        return ResponseEntity.ok(boardService.searchBoards(keyword));
    }

    // 게시글 작성
    // POST /api/members/{memberId}/boards
    @PostMapping("/members/{memberId}/boards")
    public ResponseEntity<BoardResponseDto> createBoard(
            @PathVariable Long memberId,
            @Valid @RequestBody BoardRequestDto.Create dto) {
        return ResponseEntity.ok(boardService.createBoard(memberId, dto));
    }

    // 게시글 수정
    // PUT /api/boards/{boardId}
    @PutMapping("/boards/{boardId}")
    public ResponseEntity<BoardResponseDto> updateBoard(
            @PathVariable Long boardId,
            @RequestBody BoardRequestDto.Update dto) {
        return ResponseEntity.ok(boardService.updateBoard(boardId, dto));
    }

    // 게시글 삭제 (소프트 삭제)
    // DELETE /api/boards/{boardId}
    // 204 No Content: 처리 성공했지만 반환할 데이터 없음
    @DeleteMapping("/boards/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long boardId) {
        boardService.deleteBoard(boardId);
        return ResponseEntity.noContent().build();
    }

    // 관리자 - 게시글 삭제
    // DELETE /api/admin/boards/{boardId}
    @DeleteMapping("/admin/boards/{boardId}")
    public ResponseEntity<Void> adminDeleteBoard(@PathVariable Long boardId) {
        boardService.deleteBoard(boardId);
        return ResponseEntity.noContent().build();
    }
}