package com.forme.shop.qna.service;

import com.forme.shop.common.security.SecurityUtil;
import com.forme.shop.member.entity.Member;
import com.forme.shop.member.service.MemberService;
import com.forme.shop.product.entity.Product;
import com.forme.shop.product.repository.ProductRepository;
import com.forme.shop.qna.dto.QnaRequestDto;
import com.forme.shop.qna.dto.QnaResponseDto;
import com.forme.shop.qna.entity.Qna;
import com.forme.shop.qna.repository.QnaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
    private final MemberService memberService;
    private final ProductRepository productRepository;

    // 이 요청을 보낸 사람이 이 Q&A의 비밀글 내용을 볼 수 있는지 (작성자 본인 또는 관리자)
    private boolean canViewSecret(Qna qna) {
        if (SecurityUtil.isAdmin()) return true;
        String current = SecurityUtil.getCurrentEmail();
        return current != null && current.equals(qna.getMember().getEmail());
    }

    // 전체 Q&A 목록 조회 (일반회원)
    // is_active = true 인 Q&A 만 최신순으로 반환. 비밀글은 작성자 본인/관리자가 아니면
    // content/answer를 가려서 내려준다(목록 자체는 누구나 볼 수 있되 내용은 비공개).
    public List<QnaResponseDto> getAllQna() {
        return qnaRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(qna -> QnaResponseDto.from(qna, canViewSecret(qna)))
                .collect(Collectors.toList());
    }

    // 특정 회원의 Q&A 목록 조회 ("내 문의" — 본인 또는 관리자만 조회 가능)
    // member_id 로 필터링, is_active = true 인 것만 최신순 반환
    public List<QnaResponseDto> getMyQna(Long memberId) {
        // 본인(또는 관리자)만 조회 가능 — 그렇지 않으면 다른 회원의 문의(비밀글 포함)를
        // memberId만 바꿔서 그대로 열람할 수 있었음. 존재 여부 확인과 소유자 확인을 한 번에
        // 처리하는 이유는 MemberService.findSelfOrAdminMember() 주석 참고 (회원 id 열거 방지)
        memberService.findSelfOrAdminMember(memberId);

        // 전부 본인(또는 관리자가 조회하는 본인) 소유이므로 가릴 필요 없이 그대로 반환
        return qnaRepository.findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(memberId)
                .stream()
                .map(QnaResponseDto::from)
                .collect(Collectors.toList());
    }

    // 특정 상품의 Q&A 목록 조회 — 비밀글 내용은 작성자 본인/관리자가 아니면 가려서 내려줌
    // product_id 로 필터링, is_active = true 인 것만 최신순 반환
    public List<QnaResponseDto> getProductQna(Long productId) {
        return qnaRepository.findByProductIdAndIsActiveTrueOrderByCreatedAtDesc(productId)
                .stream()
                .map(qna -> QnaResponseDto.from(qna, canViewSecret(qna)))
                .collect(Collectors.toList());
    }

    // Q&A 단건 조회 — 비밀글이면 작성자 본인/관리자가 아닐 때 content/answer를 가림
    public QnaResponseDto getQna(Long qnaId) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Q&A입니다."));
        return QnaResponseDto.from(qna, canViewSecret(qna));
    }

    // 질문 등록 (일반회원)
    @Transactional
    public QnaResponseDto createQna(Long memberId, QnaRequestDto.Create dto) {

        // 본인(또는 관리자) 명의로만 질문 등록 가능
        Member member = memberService.findSelfOrAdminMember(memberId);

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
        Qna qna = findSelfOrAdminQna(qnaId);

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
        Qna qna = findSelfOrAdminQna(qnaId);

        qna.setIsActive(false);  // 비활성화 처리 (소프트 삭제)
    }

    // "Q&A id로 대상을 찾되, 본인 것이거나 관리자일 때만 결과를 내어준다"를 한 번에 처리한다.
    // MemberService.findSelfOrAdminMember()와 같은 이유(존재 여부 열거 방지) — 대상을 먼저
    // 조회해서 없으면 400, 있는데 내 게 아니면 403을 따로 응답하면, 로그인만 한 상태로 남의
    // qnaId를 넣어봤을 때 그 응답 차이만으로 유효한 Q&A id를 하나씩 찾아낼 수 있었다.
    private Qna findSelfOrAdminQna(Long qnaId) {
        Qna qna = qnaRepository.findById(qnaId).orElse(null);
        if (SecurityUtil.isAdmin()) {
            if (qna == null) {
                throw new IllegalArgumentException("존재하지 않는 Q&A입니다.");
            }
            return qna;
        }
        if (qna == null || !SecurityUtil.isSelf(qna.getMember().getEmail())) {
            throw new AccessDeniedException("본인의 정보만 접근할 수 있습니다.");
        }
        return qna;
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