package com.forme.shop.product.service;

import com.forme.shop.product.dto.ProductRequestDto;
import com.forme.shop.product.dto.ProductResponseDto;
import com.forme.shop.product.entity.Product;
import com.forme.shop.product.repository.ProductRepository;
import com.forme.shop.category.entity.Category;
import com.forme.shop.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // 기본적으로 읽기 전용 트랜잭션 (조회 성능 최적화)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // application.yml 의 file.upload-dir 값 주입
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 전체 상품 목록 조회 (일반회원)
    // is_active = true 인 상품만 반환 (삭제된 상품 제외)
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findByIsActiveTrue()
                .stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    // 상품 단건 조회 (일반회원)
    // is_active = false 인 상품은 예외 발생
    public ProductResponseDto getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 삭제된 상품 접근 차단
        if (!product.getIsActive()) {
            throw new IllegalArgumentException("삭제된 상품입니다.");
        }
        return ProductResponseDto.from(product);
    }

    // 카테고리별 상품 조회
    // category_id 로 필터링, 삭제된 상품 제외
    public List<ProductResponseDto> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndIsActiveTrue(categoryId)
                .stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    // 상품 검색 (상품명 키워드 검색)
    // 삭제된 상품 제외
    public List<ProductResponseDto> searchProducts(String keyword) {
        return productRepository.findByNameContainingAndIsActiveTrue(keyword)
                .stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    // 메인 페이지 - 신상품 4건
    // is_new = true AND is_active = true 인 상품 최신순 4건
    public List<ProductResponseDto> getNewProducts() {
        return productRepository.findTop4ByIsNewTrueAndIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    // 메인 페이지 - 베스트 4건
    // is_best = true AND is_active = true 인 상품 최신순 4건
    public List<ProductResponseDto> getBestProducts() {
        return productRepository.findTop4ByIsBestTrueAndIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    // 메인 페이지 - 추천 3건
    // is_recommend = true AND is_active = true 인 상품 최신순 3건
    public List<ProductResponseDto> getRecommendProducts() {
        return productRepository.findTop3ByIsRecommendTrueAndIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    // 상품 등록 (관리자) - 이미지 업로드 포함
    // @RequestPart 로 JSON + 파일을 같이 받음
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto.Create dto,
                                            MultipartFile image) throws IOException {
        // 카테고리 존재 여부 확인
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        String imageUrl = null;

        // 이미지 파일이 있으면 업로드 처리
        if (image != null && !image.isEmpty()) {
            imageUrl = saveImage(image);
        }

        Product product = Product.builder()
                .category(category)
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .imageUrl(imageUrl)
                .isNew(dto.getIsNew() != null ? dto.getIsNew() : false)
                .isBest(dto.getIsBest() != null ? dto.getIsBest() : false)
                .isRecommend(dto.getIsRecommend() != null ? dto.getIsRecommend() : false)
                .build();

        return ProductResponseDto.from(productRepository.save(product));
    }

    // 상품 수정 (관리자)
    // null 체크 후 값이 있을 때만 수정 (부분 수정 가능)
    @Transactional
    public ProductResponseDto updateProduct(Long id,
                                            ProductRequestDto.Update dto,
                                            MultipartFile image) throws IOException {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 카테고리 변경 요청이 있으면 카테고리 교체
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
            product.setCategory(category);
        }

        if (dto.getName()        != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getPrice()       != null) product.setPrice(dto.getPrice());
        if (dto.getStock()       != null) product.setStock(dto.getStock());
        if (dto.getIsNew()       != null) product.setIsNew(dto.getIsNew());
        if (dto.getIsBest()      != null) product.setIsBest(dto.getIsBest());
        if (dto.getIsRecommend() != null) product.setIsRecommend(dto.getIsRecommend());

        // 새 이미지가 있으면 기존 이미지 교체
        if (image != null && !image.isEmpty()) {
            product.setImageUrl(saveImage(image));
        }

        // @Transactional 덕분에 save() 없이도 변경사항 자동 반영 (더티 체킹)
        return ProductResponseDto.from(product);
    }

    // 상품 삭제 (관리자) - 소프트 삭제
    // DB 에서 실제 삭제 안 하고 is_active = false 로 변경
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        product.setIsActive(false);  // 비활성화 처리
    }

    // 이미지 파일 저장
    // UUID 로 파일명 중복 방지
    // 저장 경로: application.yml 의 file.upload-dir
    private String saveImage(MultipartFile image) throws IOException {
        // 업로드 폴더 없으면 자동 생성
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        // UUID + 원본 파일명으로 중복 방지
        String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
        File dest = new File(uploadDir + File.separator + fileName);

        // 파일 저장
        image.transferTo(dest);

        // 저장된 파일 경로 반환 (프론트에서 이미지 URL 로 사용)
        return "/uploads/" + fileName;
    }
}