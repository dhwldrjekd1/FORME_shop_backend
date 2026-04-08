package com.bluebottle.shop.category.service;

import com.bluebottle.shop.category.dto.CategoryRequestDto;
import com.bluebottle.shop.category.dto.CategoryResponseDto;
import com.bluebottle.shop.category.entity.Category;
import com.bluebottle.shop.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // 기본적으로 읽기 전용 트랜잭션 (조회 성능 최적화)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // 활성 카테고리 목록 조회 (일반회원)
    // is_active = true 인 카테고리만 sort_order 순으로 반환
    public List<CategoryResponseDto> getCategories() {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(CategoryResponseDto::from)
                .collect(Collectors.toList());
    }

    // 전체 카테고리 목록 조회 (관리자)
    // 비활성 포함 전체 조회
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(CategoryResponseDto::from)
                .collect(Collectors.toList());
    }

    // 카테고리 단건 조회
    public CategoryResponseDto getCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
        return CategoryResponseDto.from(category);
    }

    // 카테고리 등록 (관리자)
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto.Create dto) {
        Category category = Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build();
        return CategoryResponseDto.from(categoryRepository.save(category));
    }

    // 카테고리 수정 (관리자)
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto.Update dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        // null 체크 후 값이 있을 때만 수정 (부분 수정 가능)
        if (dto.getName()        != null) category.setName(dto.getName());
        if (dto.getDescription() != null) category.setDescription(dto.getDescription());
        if (dto.getSortOrder()   != null) category.setSortOrder(dto.getSortOrder());
        if (dto.getIsActive()    != null) category.setIsActive(dto.getIsActive());

        // @Transactional 덕분에 save() 없이도 변경사항 자동 반영 (더티 체킹)
        return CategoryResponseDto.from(category);
    }

    // 카테고리 삭제 (관리자) - 소프트 삭제
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
        category.setIsActive(false);  // 비활성화 처리
    }
}