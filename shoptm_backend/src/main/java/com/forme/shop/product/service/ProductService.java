package com.forme.shop.product.service;

import com.forme.shop.product.dto.ProductRequestDto;
import com.forme.shop.product.dto.ProductResponseDto;
import com.forme.shop.product.entity.Product;
import com.forme.shop.product.entity.ProductSize;
import com.forme.shop.product.repository.ProductRepository;
import com.forme.shop.common.util.LikeEscapeUtil;
import com.forme.shop.category.entity.Category;
import com.forme.shop.category.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // 기본적으로 읽기 전용 트랜잭션 (조회 성능 최적화)
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final EntityManager entityManager;

    // saveAndFlush()가 던진 DataIntegrityViolationException이 정말 브랜드당 추천상품 유니크
    // 인덱스(products_brand_recommend_uk) 위반인지 확인한다. 같은 flush에 다른 상품들의
    // isRecommend/curatorImageUrl 변경(위에서 기존 추천을 해제한 것)도 함께 반영되므로, 무관한
    // 제약 위반(다른 CHECK/FK/유니크 등)까지 전부 "다른 관리자가 방금 바꿨다"는 안내로
    // 뭉뚱그리면 실제 원인을 못 찾게 된다 — 이 인덱스 위반일 때만 그 안내를 쓰고, 아니면
    // 그대로 다시 던져 원래 예외(→ 일반 500 처리)로 흘려보낸다.
    // 주의: 이 예외를 잡는 catch 블록 뒤에 같은 트랜잭션에서 DB 문장을 더 실행하면 안 된다 —
    // Postgres는 제약 위반이 나면 그 트랜잭션 전체를 abort 상태로 만들어서, 그 뒤에 어떤 쿼리를
    // 보내도 "current transaction is aborted" 에러만 남. 예외를 잡은 뒤엔 바로 던지고 끝내야 한다
    // (장바구니 담기에서 재시도 로직을 넣었다가 세션이 오염된 걸 겪고 원자적 upsert로 바꾼 것과
    // 같은 이유 — 여기는 재시도가 없어 안전하지만, 나중에 이 catch 뒤에 코드를 추가하지 말 것).
    private static boolean isBrandRecommendConflict(DataIntegrityViolationException e) {
        // getConstraintName()은 Hibernate가 PostgreSQL의 제약 위반 오류에서 실제 제약 이름을
        // 파싱해 채워주는 구조화된 값이라, JDBC 에러 메시지 문구에 의존하는 문자열 검사보다 안전하다.
        if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException cve) {
            return "products_brand_recommend_uk".equals(cve.getConstraintName());
        }
        return false;
    }

    // originalPrice(할인 전 가격, 화면에 취소선으로 표시)가 실제 판매가(price)보다 낮으면
    // "할인 전 가격이 지금 가격보다 싼" 앞뒤가 안 맞는 표시가 된다. 실제 결제 금액은
    // price와 discountRate로만 계산되고 originalPrice는 순수 표시용이라 결제 사고로
    // 이어지진 않지만, 관리자가 입력 실수를 등록 시점에 바로 알 수 있도록 막는다.
    // originalPrice가 없으면(할인 전 가격을 안 쓰는 상품) 검사하지 않는다.
    private static void requireOriginalPriceNotBelowPrice(Integer originalPrice, Integer price) {
        if (originalPrice != null && price != null && originalPrice < price) {
            throw new IllegalArgumentException("할인 전 가격은 현재 가격보다 낮을 수 없습니다.");
        }
    }

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
    // 삭제된 상품 제외 — LikeEscapeUtil 참고 (와일드카드 문자를 그대로 넘기면 검색이
    // 부정확해지는 문제를 막기 위한 이스케이프).
    public List<ProductResponseDto> searchProducts(String keyword) {
        return productRepository.searchByName(LikeEscapeUtil.escape(keyword))
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

    // 메인 페이지 - 추천 상품 (브랜드 순서 고정: BEANPOLE → CARHARTT → LEVI'S → DICKIES)
    private static final List<String> BRAND_ORDER = List.of("BEANPOLE", "CARHARTT", "LEVI'S", "DICKIES");

    public List<ProductResponseDto> getRecommendProducts() {
        List<ProductResponseDto> list = productRepository.findByIsRecommendTrueAndIsActiveTrueOrderByIdAsc()
                .stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
        // 브랜드 순서대로 정렬
        list.sort((a, b) -> {
            int ia = BRAND_ORDER.indexOf(a.getBrand());
            int ib = BRAND_ORDER.indexOf(b.getBrand());
            return Integer.compare(ia < 0 ? 99 : ia, ib < 0 ? 99 : ib);
        });
        return list;
    }

    // 상품 등록 (관리자) - 다중 이미지 업로드
    // rollbackFor 지정: 기본 @Transactional은 RuntimeException/Error에서만 롤백하고 체크 예외인
    // IOException(이미지 저장 중 디스크 오류)에서는 그대로 커밋해버린다 — saveImages가 디스크의
    // 이미 저장된 파일을 지워도, DB 쪽은 그 이미지 저장 실패를 반영 못 한 채 커밋되면 앞뒤가 안
    // 맞는 상태가 남는다. IOException도 명시적으로 롤백 대상에 넣어 디스크와 DB가 같이 롤백되게 함.
    @Transactional(rollbackFor = Exception.class)
    public ProductResponseDto createProduct(ProductRequestDto.Create dto,
                                            List<MultipartFile> images) throws IOException {
        requireOriginalPriceNotBelowPrice(dto.getOriginalPrice(), dto.getPrice());

        // categoryId를 명시적으로 보냈는데 존재하지 않으면(오타, 이미 삭제된 카테고리 등) 조용히
        // 다른 카테고리로 대체하지 않고 바로 실패시킨다 — 예전엔 존재하지 않는 categoryId를
        // findById 뒤 orElseGet으로 "테이블의 아무 카테고리나 첫 번째 것"(순서 보장 없음, 심지어
        // 비활성/숨김 카테고리 포함)으로 조용히 대체해서, 관리자가 의도하지 않은 카테고리에
        // 상품이 등록돼도 아무 에러 없이 그대로 저장됐음. categoryId를 아예 안 보냈을 때만
        // 노출 순서상 첫 번째 활성 카테고리를 기본값으로 쓴다(updateProduct와 동일한 기준).
        Category category;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
        } else {
            category = categoryRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("카테고리가 없습니다. 먼저 카테고리를 등록해주세요."));
        }

        // 추천 등록 시 같은 브랜드의 기존 추천 자동 해제
        if (Boolean.TRUE.equals(dto.getIsRecommend()) && dto.getBrand() != null) {
            List<Product> existing = productRepository.findByBrandAndIsRecommendTrueAndIsActiveTrue(dto.getBrand());
            for (Product p : existing) {
                p.setIsRecommend(false);
                p.setCuratorImageUrl(null);
            }
            // 기존 추천 해제를 여기서 먼저 flush해 DB에 반영한다 — 안 그러면 아래에서 새 상품을
            // saveAndFlush()할 때 Hibernate가 같은 flush 안에서 INSERT를 UPDATE보다 항상 먼저
            // 실행해(구현이 보장하는 순서), 기존 추천이 아직 안 풀린 상태에서 새 추천 상품이 먼저
            // INSERT돼 브랜드당 추천상품 유니크 인덱스에 걸릴 수 있다 — 동시 요청이 전혀 없는
            // 정상적인 단일 요청에서도 항상 발생하는 문제라 반드시 순서를 나눠야 한다.
            productRepository.flush();
        }

        // ID 직접 지정 시 중복 체크
        if (dto.getId() != null && productRepository.existsById(dto.getId())) {
            throw new IllegalArgumentException("이미 존재하는 상품 ID입니다: " + dto.getId());
        }

        // 이미지 처리: dto에 URL이 있으면 우선 사용, 없으면 파일 업로드. 디스크에 실제로 파일을
        // 쓰는 일이라 실패 시 되돌릴 방법이 없는 단계이므로, 위의 검증들을 전부 통과한 뒤 — DB
        // 저장(바로 아래 saveAndFlush) 바로 앞에서만 실행해, 그 사이 다른 이유로 실패해 롤백될
        // 때 방금 쓴 이미지 파일이 참조할 곳 없이 디스크에 남는 경우를 최소화한다. 그래도
        // saveAndFlush 자체(브랜드 추천 유니크 충돌)는 이미지 저장 뒤에만 알 수 있어, 그 실패는
        // 아래 catch에서 별도로 정리한다.
        List<String> savedUrls = List.of();
        String imageUrl = dto.getImageUrl();
        String imageUrls = dto.getImageUrls();
        if ((imageUrl == null || imageUrl.isBlank()) && images != null && !images.isEmpty()) {
            savedUrls = saveImages(images);
            if (!savedUrls.isEmpty()) {
                imageUrl = savedUrls.get(0);
                imageUrls = String.join(",", savedUrls);
            }
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

        // saveAndFlush로 즉시 INSERT를 실행해, isRecommend=true로 새 상품을 만드는 것과 동시에
        // 같은 브랜드에 다른 상품을 추천 설정하는 요청이 겹치는 드문 경우 DB의 부분 유니크
        // 인덱스(shoptm.sql 참고) 위반을 여기서 바로 잡아낸다.
        try {
            return ProductResponseDto.from(productRepository.saveAndFlush(product));
        } catch (DataIntegrityViolationException e) {
            // 바로 위에서 저장한 이미지 파일들이 저장될 상품 없이 디스크에만 남는 것을 막는다.
            savedUrls.forEach(this::deleteUploadedFile);
            if (!isBrandRecommendConflict(e)) throw e;
            throw new IllegalArgumentException("방금 다른 관리자가 같은 브랜드의 추천상품을 변경했습니다. 새로고침 후 다시 시도해주세요.");
        }
    }

    // 상품 수정 (관리자) - 다중 이미지 업로드
    // rollbackFor: createProduct와 같은 이유(바로 위 주석 참고) — IOException도 롤백 대상으로
    // 명시해, 이미지 저장 중 디스크 오류가 나면 이미 이 메서드에서 바뀐 재고/가격 등 다른 필드
    // 변경까지 함께 롤백되게 한다.
    @Transactional(rollbackFor = Exception.class)
    public ProductResponseDto updateProduct(Long id,
                                            ProductRequestDto.Update dto,
                                            List<MultipartFile> images) throws IOException {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 관리자가 수정 화면을 열어둔 사이에 이 상품이 이미 바뀌었으면(실제 주문/취소로 재고가
        // 변하는 등) 그 변경을 덮어쓰지 않고 거부한다. 특히 사이즈별 재고는 이 메서드가 DTO의
        // 절대값으로 전체 교체하므로, 이 확인이 없으면 화면을 연 시점의 오래된 재고 값으로
        // 그 사이의 실제 판매분을 조용히 지워버릴 수 있었다(교차검증에서 발견 — 490bb2f로
        // 사이즈별 재고가 실제 주문 재고 관리의 기준이 되면서 이 문제가 진짜 오버셀로 이어질
        // 수 있게 됨). expectedUpdatedAt이 없으면(구버전 클라이언트 등) 이 확인을 건너뛴다.
        if (dto.getExpectedUpdatedAt() != null && !dto.getExpectedUpdatedAt().equals(product.getUpdatedAt())) {
            throw new IllegalArgumentException(
                    "다른 곳에서 이미 이 상품 정보가 변경되었습니다. 새로고침 후 다시 시도해주세요.");
        }

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
            product.setCategory(category);
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
        // 부분 수정이라 DTO 하나만으로는 판단할 수 없어(예: 이번 요청은 price만 내려도
        // originalPrice는 그대로 유지됨), 병합까지 끝난 뒤의 최종 값을 기준으로 확인한다.
        // 단, 이번 요청이 price/originalPrice 둘 다 건드리지 않았다면 검사를 건너뛴다 —
        // 이 검증이 생기기 전에 이미 어긋난 값으로 저장된 예전 상품이 있다면, 설명만 고치는
        // 것 같은 무관한 수정까지 이 검사 때문에 막혀버리는 걸 피하기 위함.
        if (dto.getPrice() != null || dto.getOriginalPrice() != null) {
            requireOriginalPriceNotBelowPrice(product.getOriginalPrice(), product.getPrice());
        }
        if (dto.getThumbnailUrl()   != null) product.setThumbnailUrl(dto.getThumbnailUrl());
        if (dto.getColorName()      != null) product.setColorName(dto.getColorName());
        if (dto.getColorHex()       != null) product.setColorHex(dto.getColorHex());
        if (dto.getFeatures()       != null) product.setFeatures(dto.getFeatures());
        if (dto.getComposition()    != null) product.setComposition(dto.getComposition());
        if (dto.getIsNew()          != null) product.setIsNew(dto.getIsNew());
        if (dto.getIsBest()         != null) product.setIsBest(dto.getIsBest());

        // 사이즈별 재고 갱신 (전체 교체)
        if (dto.getSizeStocks() != null) {
            if (product.getSizes() != null) product.getSizes().clear();
            else product.setSizes(new java.util.ArrayList<>());
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

        // 이미지 처리: dto에 URL이 있으면 우선 사용
        if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            product.setImageUrl(dto.getImageUrl());
        }
        if (dto.getImageUrls() != null && !dto.getImageUrls().isBlank()) {
            product.setImageUrls(dto.getImageUrls());
        }

        // 파일 업로드가 있으면 기존 이미지 교체
        List<String> savedUrls = List.of();
        if (images != null && !images.isEmpty()) {
            savedUrls = saveImages(images);
            if (!savedUrls.isEmpty()) {
                product.setImageUrl(savedUrls.get(0));
                product.setImageUrls(String.join(",", savedUrls));
            }
        }

        // 이 메서드는 그동안 saveAndFlush 없이 더티체킹에만 맡겨 커밋 시점(메서드 반환 이후)에야
        // UPDATE가 나가게 했는데, 그러면 brand를 바꾸는 수정이 (createProduct/setRecommend처럼
        // 기존 추천을 해제하는 로직이 updateProduct엔 없어서) 브랜드당 추천상품 유니크 인덱스에
        // 걸릴 경우 그 실패가 이 메서드 try/catch 바깥에서 터져 방금 저장한 이미지 파일이 치울
        // 방법 없이 디스크에 남고, 사용자에게도 다른 메서드들과 다른 날것의 에러가 그대로 노출됐다.
        // saveAndFlush로 커밋을 이 메서드 안으로 끌어와 같은 방식으로 처리한다.
        try {
            return ProductResponseDto.from(productRepository.saveAndFlush(product));
        } catch (DataIntegrityViolationException e) {
            savedUrls.forEach(this::deleteUploadedFile);
            if (!isBrandRecommendConflict(e)) throw e;
            throw new IllegalArgumentException("방금 다른 관리자가 같은 브랜드의 추천상품을 변경했습니다. 새로고침 후 다시 시도해주세요.");
        }
    }

    // 추천 전체 초기화 (모든 상품 대상)
    @Transactional
    public void resetAllRecommend() {
        List<Product> all = productRepository.findAll();
        for (Product p : all) {
            p.setIsRecommend(false);
            p.setCuratorImageUrl(null);
        }
    }

    // 큐레이터 설정 전용 (추천 체크 + 큐레이터 이미지)
    @Transactional
    public ProductResponseDto setRecommend(Long productId, String curatorImageUrl) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 같은 브랜드 기존 추천 해제
        if (product.getBrand() != null) {
            List<Product> existing = productRepository.findByBrandAndIsRecommendTrueAndIsActiveTrue(product.getBrand());
            for (Product p : existing) {
                p.setIsRecommend(false);
                p.setCuratorImageUrl(null);
            }
            // 기존 추천 해제를 먼저 flush해 DB에 반영한 뒤에 새 추천을 설정한다 — 안 그러면
            // 같은 flush 안에서 여러 UPDATE의 실행 순서가 코드 순서대로 보장되지 않아(Hibernate
            // 구현 세부사항), product의 UPDATE가 existing의 UPDATE보다 먼저 나가 기존 추천이
            // 아직 안 풀린 상태에서 새 추천이 먼저 반영될 수 있다 — 동시 요청 없이도 발생 가능.
            productRepository.flush();
        }

        product.setIsRecommend(true);
        product.setCuratorImageUrl(curatorImageUrl);

        // saveAndFlush로 즉시 UPDATE를 실행해, DB의 부분 유니크 인덱스(브랜드당 추천상품 1개,
        // shoptm.sql 참고)를 위반하면 그 예외를 여기서 바로 잡아낸다. 서로 다른 상품을 같은
        // 브랜드로 동시에 추천 설정하는 두 요청 중 나중에 도착한 쪽에서만 발생하는 드문 경우.
        try {
            productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException e) {
            if (!isBrandRecommendConflict(e)) throw e;
            throw new IllegalArgumentException("방금 다른 관리자가 같은 브랜드의 추천상품을 변경했습니다. 새로고침 후 다시 시도해주세요.");
        }
        return ProductResponseDto.from(product);
    }

    // 큐레이터 해제 전용
    @Transactional
    public void unsetRecommend(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        product.setIsRecommend(false);
        product.setCuratorImageUrl(null);
    }

    // 상품 삭제 (관리자) - 소프트 삭제
    // DB 에서 실제 삭제 안 하고 is_active = false 로 변경
    // (하드 삭제하면 이 상품을 참조하는 과거 주문/리뷰/Q&A/찜이 깨지거나 함께 사라짐)
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        product.setIsActive(false);
    }

    // 상품 ID 변경 (네이티브 SQL)
    @Transactional
    public void changeProductId(Long oldId, Long newId) {
        if (oldId.equals(newId)) throw new IllegalArgumentException("동일한 ID입니다.");
        if (productRepository.existsById(newId)) throw new IllegalArgumentException("이미 존재하는 ID입니다: " + newId);
        if (!productRepository.existsById(oldId)) throw new IllegalArgumentException("존재하지 않는 상품입니다: " + oldId);

        // products.id를 참조하는 모든 테이블(cart_items/product_sizes/wishlists/order_items/
        // qna/reviews) FK에 ON UPDATE CASCADE를 걸어뒀으므로(shoptm.sql 참고), products.id 하나만
        // 바꾸면 나머지는 DB가 알아서 같이 옮겨준다. 예전엔 product_sizes만 애플리케이션에서 직접
        // 옮기고 나머지는 빠뜨려서, 이 상품이 한 번이라도 주문/찜/문의/리뷰된 적이 있으면(=실사용
        // 상품 대부분) FK 위반으로 항상 실패했다.
        entityManager.createNativeQuery("UPDATE products SET id = :newId WHERE id = :oldId")
                .setParameter("newId", newId).setParameter("oldId", oldId).executeUpdate();
        entityManager.createNativeQuery("SELECT setval('products_id_seq', (SELECT COALESCE(MAX(id), 1) FROM products))")
                .getSingleResult();
    }

    // 허용할 이미지 확장자 (대소문자 무관)
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS =
            List.of("jpg", "jpeg", "png", "gif", "webp");

    // 여러 이미지를 순서대로 저장한다. 파일 저장은 DB 트랜잭션 밖(디스크)의 일이라, 뒤쪽
    // 이미지에서 확장자/매직바이트 검증 실패(IllegalArgumentException)나 디스크 오류
    // (IOException)가 나서 이 메서드가 예외를 던지면 호출부의 @Transactional이 DB는 롤백해도
    // 이미 디스크에 써진 앞쪽 이미지들은 그대로 남아 고아 파일이 된다 — 그래서 실패 시 이번
    // 호출에서 이미 저장한 파일들을 직접 지우고 나서야 예외를 다시 던진다.
    private List<String> saveImages(List<MultipartFile> images) throws IOException {
        List<String> urls = new java.util.ArrayList<>();
        try {
            for (MultipartFile img : images) {
                if (img != null && !img.isEmpty()) {
                    urls.add(saveImage(img));
                }
            }
            return urls;
        } catch (IOException | RuntimeException e) {
            for (String url : urls) {
                deleteUploadedFile(url);
            }
            throw e;
        }
    }

    // saveImages의 정리 과정에서 실제로 지우지 못한 파일이 있으면(권한 문제, 이미 없어짐 등)
    // 그 사실 자체는 운영에서 고아 파일을 추적할 유일한 흔적이라 로그로 남긴다. delete()는
    // 실패해도 예외를 던지지 않으므로 반환값을 반드시 확인해야 한다.
    private void deleteUploadedFile(String url) {
        File file = new File(new File(uploadDir).getAbsoluteFile(), url.substring("/uploads/".length()));
        // delete()는 삭제 실패와 "원래 거기 파일이 없었음"을 둘 다 false로 뭉뚱그려 반환한다 —
        // transferTo가 파일을 쓰기 전에 실패한 경우(권한 오류 등) 지울 파일 자체가 없는 게
        // 정상이므로, exists()로 먼저 걸러내 그런 경우까지 "삭제 실패"로 잘못 경고하지 않는다.
        if (file.exists() && !file.delete()) {
            log.warn("이미지 업로드 실패 후 정리 중 파일 삭제 실패 — 수동 확인 필요: {}", file.getAbsolutePath());
        }
    }

    // 이미지 파일 저장
    // 파일명은 원본 파일명을 전혀 사용하지 않고 "UUID.확장자" 형태로만 생성한다.
    // 원본 파일명(image.getOriginalFilename())은 클라이언트가 임의로 조작해 보낼 수 있는 값이라
    // "../../etc/cron.d/evil" 같은 경로 조작 문자열이 섞여 있어도 그대로 믿으면 안 된다.
    private String saveImage(MultipartFile image) throws IOException {
        String ext = extractAllowedExtension(image.getOriginalFilename());
        validateImageContent(image, ext);

        // 상대경로(dir)를 그대로 transferTo에 넘기면 Spring이 앱 작업 폴더가 아니라
        // 서블릿 컨테이너의 임시 작업 폴더 기준으로 저장해버려 파일이 사라진다.
        // getAbsoluteFile()로 절대경로를 넘겨야 우리가 의도한 uploadDir에 저장된다.
        File dir = new File(uploadDir).getAbsoluteFile();
        if (!dir.exists()) dir.mkdirs();

        String fileName = UUID.randomUUID() + "." + ext;
        File dest = new File(dir, fileName);

        // transferTo가 바이트를 일부만 쓴 채로 실패(디스크 가득 참 등)할 수 있다 — 이 경우
        // saveImages의 정리 루프는 이 파일의 URL을 아직 모르니(반환 전에 예외가 났으므로) 지울
        // 수 없다. 그래서 실패 시 여기서 바로 그 반쪽짜리 파일을 지운다.
        try {
            image.transferTo(dest);
        } catch (IOException e) {
            deleteUploadedFile("/uploads/" + fileName);
            throw e;
        }

        return "/uploads/" + fileName;
    }

    // 원본 파일명에서 확장자만 뽑아 화이트리스트로 검증
    // (경로 구분자나 상위 폴더 이동 문자열은 확장자로 취급되지 않으므로 자동으로 걸러짐)
    private String extractAllowedExtension(String originalFilename) {
        if (originalFilename == null) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            throw new IllegalArgumentException("확장자가 없는 파일은 업로드할 수 없습니다.");
        }
        String ext = originalFilename.substring(dotIndex + 1).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다: " + ext);
        }
        return ext;
    }

    // 파일명 확장자는 클라이언트가 마음대로 붙일 수 있으므로(예: 실행 파일을 .png로 위장),
    // 실제 파일 앞부분 시그니처(매직 바이트)를 읽어 확장자가 주장하는 형식과 일치하는지 검증한다.
    private void validateImageContent(MultipartFile image, String ext) throws IOException {
        byte[] header = new byte[12];
        int read;
        try (InputStream in = image.getInputStream()) {
            read = in.readNBytes(header, 0, header.length);
        }

        boolean valid = switch (ext) {
            case "jpg", "jpeg" -> read >= 3
                    && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            case "png" -> read >= 8
                    && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                    && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A;
            case "gif" -> read >= 4
                    && header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38;
            case "webp" -> read >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };

        if (!valid) {
            throw new IllegalArgumentException("파일 내용이 " + ext + " 형식과 일치하지 않습니다.");
        }
    }
}