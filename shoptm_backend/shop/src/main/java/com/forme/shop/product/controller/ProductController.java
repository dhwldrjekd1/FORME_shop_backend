package com.forme.shop.product.controller;

import com.forme.shop.product.dto.ProductRequestDto;
import com.forme.shop.product.dto.ProductResponseDto;
import com.forme.shop.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 전체 상품 목록 (일반회원)
    // GET /api/products
    @GetMapping("/products")
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // 상품 단건 조회 (일반회원)
    // GET /api/products/{id}
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    // 카테고리별 상품 조회
    // GET /api/products/category?category=의류
    @GetMapping("/products/category")
    public ResponseEntity<List<ProductResponseDto>> getByCategory(
            @RequestParam Long categoryId) {  // String → Long 으로 변경
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    // 상품 검색
    // GET /api/products/search?keyword=티셔츠
    @GetMapping("/products/search")
    public ResponseEntity<List<ProductResponseDto>> search(
            @RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    // 메인페이지 - 신상품 4개
    // GET /api/products/new
    @GetMapping("/products/new")
    public ResponseEntity<List<ProductResponseDto>> getNewProducts() {
        return ResponseEntity.ok(productService.getNewProducts());
    }

    // 메인페이지 - 베스트 4개
    // GET /api/products/best
    @GetMapping("/products/best")
    public ResponseEntity<List<ProductResponseDto>> getBestProducts() {
        return ResponseEntity.ok(productService.getBestProducts());
    }

    // 메인페이지 - 추천 4개
    // GET /api/products/recommend
    @GetMapping("/products/recommend")
    public ResponseEntity<List<ProductResponseDto>> getRecommendProducts() {
        return ResponseEntity.ok(productService.getRecommendProducts());
    }

    // 상품 등록 (관리자) - 다중 이미지 업로드
    @PostMapping("/admin/products")
    public ResponseEntity<ProductResponseDto> createProduct(
            @Valid @RequestPart ProductRequestDto.Create dto,
            @RequestPart(required = false) List<MultipartFile> images) throws IOException {
        return ResponseEntity.ok(productService.createProduct(dto, images));
    }

    // 상품 수정 (관리자) - 다중 이미지 업로드
    @PutMapping("/admin/products/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long id,
            @RequestPart ProductRequestDto.Update dto,
            @RequestPart(required = false) List<MultipartFile> images) throws IOException {
        return ResponseEntity.ok(productService.updateProduct(id, dto, images));
    }

    // 상품 삭제 (관리자)
    // DELETE /api/admin/products/{id}
    @DeleteMapping("/admin/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // 상품 ID 변경 (관리자)
    // PATCH /api/admin/products/{id}/change-id?newId=501
    @PatchMapping("/admin/products/{id}/change-id")
    public ResponseEntity<?> changeProductId(@PathVariable Long id, @RequestParam Long newId) {
        try {
            productService.changeProductId(id, newId);
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", id + " → " + newId + " 변경 완료"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }
}