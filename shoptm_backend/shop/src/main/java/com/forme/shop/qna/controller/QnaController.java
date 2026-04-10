package com.forme.shop.qna.controller;

import com.forme.shop.qna.dto.QnaRequestDto;
import com.forme.shop.qna.dto.QnaResponseDto;
import com.forme.shop.qna.service.QnaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController               // @Controller + @ResponseBody: JSON 형태로 응답 반환
@RequestMapping("/api")       // 이 컨트롤러의 모든 URL 앞에 /api 붙음
@RequiredArgsConstructor      // Lombok: final 필드 생성자 주입 자동 처리
public class QnaController {

    private final QnaService qnaService;

    // 전체 Q&A 목록 조회
    // GET /api/qna
    @GetMapping("/qna")
    public ResponseEntity<List<QnaResponseDto>> getAllQna() {
        return ResponseEntity.ok(qnaService.getAllQna());
    }

    // 특정 회원의 Q&A 목록 조회
    // GET /api/members/{memberId}/qna
    @GetMapping("/members/{memberId}/qna")
    public ResponseEntity<List<QnaResponseDto>> getMyQna(
            @PathVariable Long memberId) {
        return ResponseEntity.ok(qnaService.getMyQna(memberId));
    }

    // 특정 상품의 Q&A 목록 조회
    // GET /api/products/{productId}/qna
    @GetMapping("/products/{productId}/qna")
    public ResponseEntity<List<QnaResponseDto>> getProductQna(
            @PathVariable Long productId) {
        return ResponseEntity.ok(qnaService.getProductQna(productId));
    }

    // Q&A 단건 조회
    // GET /api/qna/{qnaId}
    @GetMapping("/qna/{qnaId}")
    public ResponseEntity<QnaResponseDto> getQna(
            @PathVariable Long qnaId) {
        return ResponseEntity.ok(qnaService.getQna(qnaId));
    }

    // 질문 등록 (일반회원)
    // POST /api/members/{memberId}/qna
    @PostMapping("/members/{memberId}/qna")
    public ResponseEntity<QnaResponseDto> createQna(
            @PathVariable Long memberId,
            @Valid @RequestBody QnaRequestDto.Create dto) {
        return ResponseEntity.ok(qnaService.createQna(memberId, dto));
    }

    // 질문 수정 (일반회원)
    // PUT /api/qna/{qnaId}
    @PutMapping("/qna/{qnaId}")
    public ResponseEntity<QnaResponseDto> updateQna(
            @PathVariable Long qnaId,
            @RequestBody QnaRequestDto.Update dto) {
        return ResponseEntity.ok(qnaService.updateQna(qnaId, dto));
    }

    // 질문 삭제 (일반회원)
    // DELETE /api/qna/{qnaId}
    // 204 No Content: 처리 성공했지만 반환할 데이터 없음
    @DeleteMapping("/qna/{qnaId}")
    public ResponseEntity<Void> deleteQna(@PathVariable Long qnaId) {
        qnaService.deleteQna(qnaId);
        return ResponseEntity.noContent().build();
    }

    // 관리자 - 답변 등록/수정
    // POST /api/admin/qna/{qnaId}/answer
    @PostMapping("/admin/qna/{qnaId}/answer")
    public ResponseEntity<QnaResponseDto> answerQna(
            @PathVariable Long qnaId,
            @Valid @RequestBody QnaRequestDto.Answer dto) {
        return ResponseEntity.ok(qnaService.answerQna(qnaId, dto));
    }

    // 관리자 - 답변 삭제
    // DELETE /api/admin/qna/{qnaId}/answer
    @DeleteMapping("/admin/qna/{qnaId}/answer")
    public ResponseEntity<QnaResponseDto> deleteAnswer(
            @PathVariable Long qnaId) {
        return ResponseEntity.ok(qnaService.deleteAnswer(qnaId));
    }

    // 관리자 - Q&A 삭제
    // DELETE /api/admin/qna/{qnaId}
    @DeleteMapping("/admin/qna/{qnaId}")
    public ResponseEntity<Void> adminDeleteQna(@PathVariable Long qnaId) {
        qnaService.deleteQna(qnaId);
        return ResponseEntity.noContent().build();
    }
}