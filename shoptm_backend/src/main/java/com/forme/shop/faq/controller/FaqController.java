package com.forme.shop.faq.controller;

import com.forme.shop.faq.dto.FaqRequestDto;
import com.forme.shop.faq.dto.FaqResponseDto;
import com.forme.shop.faq.service.FaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController               // @Controller + @ResponseBody: JSON 형태로 응답 반환
@RequestMapping("/api")       // 이 컨트롤러의 모든 URL 앞에 /api 붙음
@RequiredArgsConstructor      // Lombok: final 필드 생성자 주입 자동 처리
public class FaqController {

    private final FaqService faqService;

    // 전체 FAQ 목록 조회 (비로그인 포함 누구나)
    // GET /api/faq
    @GetMapping("/faq")
    public ResponseEntity<List<FaqResponseDto>> getAllFaq() {
        return ResponseEntity.ok(faqService.getAllFaq());
    }

    // 관리자 - FAQ 등록
    // POST /api/admin/faq
    @PostMapping("/admin/faq")
    public ResponseEntity<FaqResponseDto> createFaq(
            @Valid @RequestBody FaqRequestDto.Create dto) {
        return ResponseEntity.ok(faqService.createFaq(dto));
    }

    // 관리자 - FAQ 수정
    // PUT /api/admin/faq/{faqId}
    @PutMapping("/admin/faq/{faqId}")
    public ResponseEntity<FaqResponseDto> updateFaq(
            @PathVariable Long faqId,
            @RequestBody FaqRequestDto.Update dto) {
        return ResponseEntity.ok(faqService.updateFaq(faqId, dto));
    }

    // 관리자 - FAQ 삭제
    // DELETE /api/admin/faq/{faqId}
    @DeleteMapping("/admin/faq/{faqId}")
    public ResponseEntity<Void> deleteFaq(@PathVariable Long faqId) {
        faqService.deleteFaq(faqId);
        return ResponseEntity.noContent().build();
    }
}
