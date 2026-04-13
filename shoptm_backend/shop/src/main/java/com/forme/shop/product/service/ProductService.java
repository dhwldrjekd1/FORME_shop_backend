package com.forme.shop.product.service;

import com.forme.shop.product.dto.ProductRequestDto;
import com.forme.shop.product.dto.ProductResponseDto;
import com.forme.shop.product.entity.Product;
import com.forme.shop.product.entity.ProductSize;
import com.forme.shop.product.repository.ProductRepository;
import com.forme.shop.category.entity.Category;
import com.forme.shop.category.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

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
        return productRepository.findTop4ByIsRecommendTrueAndIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    // 상품 등록 (관리자) - 다중 이미지 업로드
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto.Create dto,
                                            List<MultipartFile> images) throws IOException {
        // categoryId가 없으면 첫 번째 카테고리 사용
        Long catId = dto.getCategoryId() != null ? dto.getCategoryId() : 1L;
        Category category = categoryRepository.findById(catId)
                .orElseGet(() -> categoryRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("카테고리가 없습니다. 먼저 카테고리를 등록해주세요.")));

        // 이미지 처리: dto에 URL이 있으면 우선 사용, 없으면 파일 업로드
        String imageUrl = dto.getImageUrl();
        String imageUrls = dto.getImageUrls();
        if ((imageUrl == null || imageUrl.isBlank()) && images != null && !images.isEmpty()) {
            List<String> urls = new java.util.ArrayList<>();
            for (MultipartFile img : images) {
                if (img != null && !img.isEmpty()) {
                    urls.add(saveImage(img));
                }
            }
            if (!urls.isEmpty()) {
                imageUrl = urls.get(0); // 대표 이미지 = 첫 번째
                imageUrls = String.join(",", urls);
            }
        }

        // 추천 등록 시 브랜드당 1개만 허용
        if (Boolean.TRUE.equals(dto.getIsRecommend()) && dto.getBrand() != null) {
            if (productRepository.existsByBrandAndIsRecommendTrueAndIsActiveTrue(dto.getBrand())) {
                throw new IllegalArgumentException(dto.getBrand() + " 브랜드에 이미 추천 상품이 있습니다. 기존 추천을 해제하고 다시 시도해주세요.");
            }
        }

        // ID 직접 지정 시 중복 체크
        if (dto.getId() != null && productRepository.existsById(dto.getId())) {
            throw new IllegalArgumentException("이미 존재하는 상품 ID입니다: " + dto.getId());
        }

        Product product = Product.builder()
                .category(category)
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .imageUrl(imageUrl)
                .imageUrls(imageUrls)
                .thumbnailUrl(dto.getThumbnailUrl())
                .curatorImageUrl(dto.getCuratorImageUrl())
                .colorName(dto.getColorName())
                .colorHex(dto.getColorHex())
                .features(dto.getFeatures())
                .composition(dto.getComposition())
                .size(dto.getSize())
                .gender(dto.getGender())
                .brand(dto.getBrand())
                .discountRate(dto.getDiscountRate())
                .originalPrice(dto.getOriginalPrice())
                .isNew(dto.getIsNew() != null ? dto.getIsNew() : false)
                .isBest(dto.getIsBest() != null ? dto.getIsBest() : false)
                .isRecommend(dto.getIsRecommend() != null ? dto.getIsRecommend() : false)
                .build();

        // ID 직접 지정
        if (dto.getId() != null) {
            product.setId(dto.getId());
        }

        // 사이즈별 재고 저장
        if (dto.getSizeStocks() != null && !dto.getSizeStocks().isEmpty()) {
            for (var ss : dto.getSizeStocks()) {
                ProductSize ps = ProductSize.builder()
                        .product(product)
                        .size(ss.getSize())
                        .stock(ss.getStock() != null ? ss.getStock() : 0)
                        .build();
                product.getSizes().add(ps);
            }
            // 전체 재고 = 사이즈별 재고 합계
            product.setStock(dto.getSizeStocks().stream()
                    .mapToInt(s -> s.getStock() != null ? s.getStock() : 0).sum());
        }

        return ProductResponseDto.from(productRepository.save(product));
    }

    // 상품 수정 (관리자) - 다중 이미지 업로드
    @Transactional
    public ProductResponseDto updateProduct(Long id,
                                            ProductRequestDto.Update dto,
                                            List<MultipartFile> images) throws IOException {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
            product.setCategory(category);
        }

        // 추천 등록 시 브랜드당 1개만 허용 (자기 자신 제외)
        if (Boolean.TRUE.equals(dto.getIsRecommend())) {
            String brand = dto.getBrand() != null ? dto.getBrand() : product.getBrand();
            if (brand != null) {
                List<Product> existing = productRepository.findByBrandAndIsRecommendTrueAndIsActiveTrue(brand);
                boolean hasDuplicate = existing.stream().anyMatch(p -> !p.getId().equals(id));
                if (hasDuplicate) {
                    throw new IllegalArgumentException(brand + " 브랜드에 이미 추천 상품이 있습니다. 기존 추천을 해제하고 다시 시도해주세요.");
                }
            }
        }

        if (dto.getName()        != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getPrice()       != null) product.setPrice(dto.getPrice());
        if (dto.getStock()       != null) product.setStock(dto.getStock());
        if (dto.getSize()        != null) product.setSize(dto.getSize());
        if (dto.getGender()      != null) product.setGender(dto.getGender());
        if (dto.getBrand()       != null) product.setBrand(dto.getBrand());
        if (dto.getDiscountRate()   != null) product.setDiscountRate(dto.getDiscountRate());
        if (dto.getOriginalPrice()  != null) product.setOriginalPrice(dto.getOriginalPrice());
        if (dto.getThumbnailUrl()   != null) product.setThumbnailUrl(dto.getThumbnailUrl());
        if (dto.getCuratorImageUrl() != null) product.setCuratorImageUrl(dto.getCuratorImageUrl());
        if (dto.getColorName()      != null) product.setColorName(dto.getColorName());
        if (dto.getColorHex()       != null) product.setColorHex(dto.getColorHex());
        if (dto.getFeatures()       != null) product.setFeatures(dto.getFeatures());
        if (dto.getComposition()    != null) product.setComposition(dto.getComposition());

        // 사이즈별 재고 갱신 (전체 교체)
        if (dto.getSizeStocks() != null) {
            product.getSizes().clear();
            for (var ss : dto.getSizeStocks()) {
                ProductSize ps = ProductSize.builder()
                        .product(product)
                        .size(ss.getSize())
                        .stock(ss.getStock() != null ? ss.getStock() : 0)
                        .build();
                product.getSizes().add(ps);
            }
            product.setStock(dto.getSizeStocks().stream()
                    .mapToInt(s -> s.getStock() != null ? s.getStock() : 0).sum());
        }
        if (dto.getIsNew()       != null) product.setIsNew(dto.getIsNew());
        if (dto.getIsBest()      != null) product.setIsBest(dto.getIsBest());
        if (dto.getIsRecommend() != null) product.setIsRecommend(dto.getIsRecommend());

        // 이미지 처리: dto에 URL이 있으면 우선 사용
        if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            product.setImageUrl(dto.getImageUrl());
        }
        if (dto.getImageUrls() != null && !dto.getImageUrls().isBlank()) {
            product.setImageUrls(dto.getImageUrls());
        }

        // 파일 업로드가 있으면 기존 이미지 교체
        if (images != null && !images.isEmpty()) {
            List<String> urls = new java.util.ArrayList<>();
            for (MultipartFile img : images) {
                if (img != null && !img.isEmpty()) {
                    urls.add(saveImage(img));
                }
            }
            if (!urls.isEmpty()) {
                product.setImageUrl(urls.get(0));
                product.setImageUrls(String.join(",", urls));
            }
        }

        return ProductResponseDto.from(product);
    }

    // 상품 삭제 (관리자) - 소프트 삭제
    // DB 에서 실제 삭제 안 하고 is_active = false 로 변경
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        productRepository.delete(product);  // DB에서 완전 삭제
    }

    // 상품 ID 변경 (네이티브 SQL)
    @Transactional
    public void changeProductId(Long oldId, Long newId) {
        if (oldId.equals(newId)) throw new IllegalArgumentException("동일한 ID입니다.");
        if (productRepository.existsById(newId)) throw new IllegalArgumentException("이미 존재하는 ID입니다: " + newId);
        if (!productRepository.existsById(oldId)) throw new IllegalArgumentException("존재하지 않는 상품입니다: " + oldId);

        // 외래키 제약조건 때문에 순서 중요: sizes → products
        entityManager.createNativeQuery("UPDATE product_sizes SET product_id = :newId WHERE product_id = :oldId")
                .setParameter("newId", newId).setParameter("oldId", oldId).executeUpdate();
        entityManager.createNativeQuery("UPDATE products SET id = :newId WHERE id = :oldId")
                .setParameter("newId", newId).setParameter("oldId", oldId).executeUpdate();
        entityManager.createNativeQuery("SELECT setval('products_id_seq', (SELECT COALESCE(MAX(id), 1) FROM products))")
                .getSingleResult();
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