package com.forme.shop.faq.service;

import com.forme.shop.faq.dto.FaqRequestDto;
import com.forme.shop.faq.dto.FaqResponseDto;
import com.forme.shop.faq.entity.Faq;
import com.forme.shop.faq.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // 기본적으로 읽기 전용 트랜잭션 (조회 성능 최적화)
public class FaqService {

    private final FaqRepository faqRepository;

    // 전체 FAQ 목록 조회 (정렬 순서 -> id 순)
    public List<FaqResponseDto> getAllFaq() {
        return faqRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream()
                .map(FaqResponseDto::from)
                .collect(Collectors.toList());
    }

    // FAQ 등록 (관리자)
    @Transactional
    public FaqResponseDto createFaq(FaqRequestDto.Create dto) {
        Faq faq = Faq.builder()
                .category(dto.getCategory())
                .question(dto.getQuestion())
                .answer(dto.getAnswer())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build();

        return FaqResponseDto.from(faqRepository.save(faq));
    }

    // FAQ 수정 (관리자)
    @Transactional
    public FaqResponseDto updateFaq(Long faqId, FaqRequestDto.Update dto) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 FAQ입니다."));

        // null 체크 후 값이 있을 때만 수정 (부분 수정 가능)
        if (dto.getCategory()  != null) faq.setCategory(dto.getCategory());
        if (dto.getQuestion()  != null) faq.setQuestion(dto.getQuestion());
        if (dto.getAnswer()    != null) faq.setAnswer(dto.getAnswer());
        if (dto.getSortOrder() != null) faq.setSortOrder(dto.getSortOrder());

        // @Transactional 덕분에 save() 없이도 변경사항 자동 반영 (더티 체킹)
        return FaqResponseDto.from(faq);
    }

    // FAQ 삭제 (관리자)
    @Transactional
    public void deleteFaq(Long faqId) {
        if (!faqRepository.existsById(faqId)) {
            throw new IllegalArgumentException("존재하지 않는 FAQ입니다.");
        }
        faqRepository.deleteById(faqId);
    }
}
