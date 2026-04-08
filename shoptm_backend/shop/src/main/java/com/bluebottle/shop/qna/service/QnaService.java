package com.bluebottle.shop.qna.service;

import com.bluebottle.shop.member.entity.Member;
import com.bluebottle.shop.member.repository.MemberRepository;
import com.bluebottle.shop.product.entity.Product;
import com.bluebottle.shop.product.repository.ProductRepository;
import com.bluebottle.shop.qna.dto.QnaRequestDto;
import com.bluebottle.shop.qna.dto.QnaResponseDto;
import com.bluebottle.shop.qna.entity.Qna;
import com.bluebottle.shop.qna.repository.QnaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // 기본적으로 읽기 전용 트랜잭션 (조회 성능 최적화)
public class QnaService {

    private final QnaRepository qnaRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    // 전체 Q&A 목록 조회 (일반회원)
    // is_active = true 인 Q&A 만 최신순으로 반환
    public List<QnaResponseDto> getAllQna() {
        return qnaRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(QnaResponseDto::from)
                .collect(Collectors.toList());
    }

    // 특정 회원의 Q&A 목록 조회 (일반회원)
    // member_id 로 필터링, is_active = true 인 것만 최신순 반환
    public List<QnaResponseDto> getMyQna(Long memberId) {
        return qnaRepository.findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(memberId)
                .stream()
                .map(QnaResponseDto::from)
                .collect(Collectors.toList());
    }

    // 특정 상품의 Q&A 목록 조회
    // product_id 로 필터링, is_active = true 인 것만 최신순 반환
    public List<QnaResponseDto> getProductQna(Long productId) {
        return qnaRepository.findByProductIdAndIsActiveTrueOrderByCreatedAtDesc(productId)
                .stream()
                .map(QnaResponseDto::from)
                .collect(Collectors.toList());
    }

    // Q&A 단건 조회
    public QnaResponseDto getQna(Long qnaId) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Q&A입니다."));
        return QnaResponseDto.from(qna);
    }

    // 질문 등록 (일반회원)
    @Transactional
    public QnaResponseDto createQna(Long memberId, QnaRequestDto.Create dto) {

        // 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 상품이 있으면 존재 여부 확인 (선택사항)
        // product_id 가 null 이면 일반 문의로 처리
        Product product = null;
        if (dto.getProductId() != null) {
            product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        }

        Qna qna = Qna.builder()
                .member(member)
                .product(product)
                .title(dto.getTitle())
                .content(dto.getContent())
                .isSecret(dto.getIsSecret() != null ? dto.getIsSecret() : false)
                .build();

        return QnaResponseDto.from(qnaRepository.save(qna));
    }

    // 질문 수정 (일반회원)
    @Transactional
    public QnaResponseDto updateQna(Long qnaId, QnaRequestDto.Update dto) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Q&A입니다."));

        // null 체크 후 값이 있을 때만 수정 (부분 수정 가능)
        if (dto.getTitle()    != null) qna.setTitle(dto.getTitle());
        if (dto.getContent()  != null) qna.setContent(dto.getContent());
        if (dto.getIsSecret() != null) qna.setIsSecret(dto.getIsSecret());

        // @Transactional 덕분에 save() 없이도 변경사항 자동 반영 (더티 체킹)
        return QnaResponseDto.from(qna);
    }

    // 질문 삭제 (일반회원 또는 관리자) - 소프트 삭제
    // DB 에서 실제 삭제 안 하고 is_active = false 로 변경
    @Transactional
    public void deleteQna(Long qnaId) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Q&A입니다."));
        qna.setIsActive(false);  // 비활성화 처리 (소프트 삭제)
    }

    // 답변 등록/수정 (관리자)
    // 답변 등록 시 status = ANSWERED 로 변경
    @Transactional
    public QnaResponseDto answerQna(Long qnaId, QnaRequestDto.Answer dto) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Q&A입니다."));

        qna.setAnswer(dto.getAnswer());           // 답변 내용 저장
        qna.setStatus("ANSWERED");                // 답변 상태 변경
        qna.setAnsweredAt(LocalDateTime.now());    // 답변 시간 저장

        // @Transactional 덕분에 save() 없이도 변경사항 자동 반영 (더티 체킹)
        return QnaResponseDto.from(qna);
    }

    // 답변 삭제 (관리자)
    // 답변 삭제 시 status = PENDING 으로 초기화
    @Transactional
    public QnaResponseDto deleteAnswer(Long qnaId) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Q&A입니다."));

        qna.setAnswer(null);         // 답변 내용 삭제
        qna.setStatus("PENDING");    // 미답변 상태로 초기화
        qna.setAnsweredAt(null);     // 답변 시간 초기화

        return QnaResponseDto.from(qna);
    }
}