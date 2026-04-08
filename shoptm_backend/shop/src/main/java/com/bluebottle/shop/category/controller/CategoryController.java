package com.bluebottle.shop.category.controller;

import com.bluebottle.shop.category.dto.CategoryRequestDto;
import com.bluebottle.shop.category.dto.CategoryResponseDto;
import com.bluebottle.shop.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController               // @Controller + @ResponseBody: JSON 형태로 응답 반환
@RequestMapping("/api")       // 이 컨트롤러의 모든 URL 앞에 /api 붙음
@RequiredArgsConstructor      // Lombok: final 필드 생성자 주입 자동 처리
public class CategoryController {

    private final CategoryService categoryService;

    // 활성 카테고리 목록 조회 (일반회원)
    // GET /api/categories
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponseDto>> getCategories() {
        return ResponseEntity.ok(categoryService.getCategories());
    }

    // 전체 카테고리 목록 조회 (관리자 - 비활성 포함)
    // GET /api/admin/categories
    @GetMapping("/admin/categories")
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    // 카테고리 단건 조회
    // GET /api/categories/{id}
    @GetMapping("/categories/{id}")
    public ResponseEntity<CategoryResponseDto> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategory(id));
    }

    // 카테고리 등록 (관리자)
    // POST /api/admin/categories
    @PostMapping("/admin/categories")
    public ResponseEntity<CategoryResponseDto> createCategory(
            @Valid @RequestBody CategoryRequestDto.Create dto) {
        return ResponseEntity.ok(categoryService.createCategory(dto));
    }

    // 카테고리 수정 (관리자)
    // PUT /api/admin/categories/{id}
    @PutMapping("/admin/categories/{id}")
    public ResponseEntity<CategoryResponseDto> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryRequestDto.Update dto) {
        return ResponseEntity.ok(categoryService.updateCategory(id, dto));
    }

    // 카테고리 삭제 (관리자) - 소프트 삭제
    // DELETE /api/admin/categories/{id}
    // 204 No Content: 처리 성공했지만 반환할 데이터 없음
    @DeleteMapping("/admin/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}