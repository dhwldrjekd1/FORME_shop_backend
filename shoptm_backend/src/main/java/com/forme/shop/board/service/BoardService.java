package com.forme.shop.board.service;

import com.forme.shop.board.dto.BoardRequestDto;
import com.forme.shop.board.dto.BoardResponseDto;
import com.forme.shop.board.entity.Board;
import com.forme.shop.board.repository.BoardRepository;
import com.forme.shop.common.security.SecurityUtil;
import com.forme.shop.member.entity.Member;
import com.forme.shop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final MemberService memberService;

    // 전체 게시글 목록 조회
    // 삭제되지 않은 게시글만 최신순으로 반환
    public List<BoardResponseDto> getAllBoards() {
        return boardRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(BoardResponseDto::from)
                .collect(Collectors.toList());
    }

    // 존재 여부·삭제 여부 확인 후 게시글을 반환. IllegalArgumentException을 쓰는 이유:
    // IllegalStateException은 GlobalExceptionHandler에 전용 처리기가 없어 500으로 떨어지는데,
    // "존재하지 않음"/"삭제됨"은 클라이언트 입장에서 동일하게 400으로 안내되는 게 맞다.
    private Board requireActiveBoard(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        if (!board.getIsActive()) {
            throw new IllegalArgumentException("삭제된 게시글입니다.");
        }
        return board;
    }

    // 게시글 단건 조회 + 조회수 증가
    // @Transactional: 조회수 증가는 DB 변경이므로 쓰기 트랜잭션 필요
    @Transactional
    public BoardResponseDto getBoard(Long id) {
        requireActiveBoard(id);

        // 조회수 1 증가 — clearAutomatically=true라 이 호출 이후 영속성 컨텍스트가 비워지므로,
        // 위에서 먼저 로드해둔 엔티티를 그대로 응답에 쓰면 안 늘어난 조회수가 그대로 나간다.
        // 반드시 증가 이후 다시 조회한 엔티티로 응답을 조립한다.
        boardRepository.incrementViews(id);

        // 위 두 조회 사이(아주 드물게) 글이 삭제되거나 작성자 탈퇴로 함께 삭제될 수 있으므로,
        // 존재 여부와 삭제 여부를 다시 조회한 엔티티 기준으로 한 번 더 확인한다.
        Board updated = requireActiveBoard(id);
        return BoardResponseDto.from(updated);
    }

    // 특정 회원의 게시글 목록 조회 ("내 글" — 작성/수정/삭제와 달리 이 조회에는 소유자 검증이
    // 통째로 빠져 있어서, 로그인한 회원이면 누구나 memberId만 바꿔서 다른 회원이 쓴 글 목록을
    // 그대로 볼 수 있었음 (이 라우트 자체는 인증이 필요해 비회원은 애초에 못 들어옴)
    public List<BoardResponseDto> getMyBoards(Long memberId) {
        memberService.findSelfOrAdminMember(memberId);

        return boardRepository.findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(memberId)
                .stream()
                .map(BoardResponseDto::from)
                .collect(Collectors.toList());
    }

    // 제목으로 게시글 검색
    public List<BoardResponseDto> searchBoards(String keyword) {
        return boardRepository.findByTitleContainingAndIsActiveTrueOrderByCreatedAtDesc(keyword)
                .stream()
                .map(BoardResponseDto::from)
                .collect(Collectors.toList());
    }

    // 게시글 작성
    @Transactional
    public BoardResponseDto createBoard(Long memberId, BoardRequestDto.Create dto) {
        // 본인(또는 관리자) 명의로만 게시글 작성 가능
        Member member = memberService.findSelfOrAdminMember(memberId);

        Board board = Board.builder()
                .member(member)
                .title(dto.getTitle())
                .content(dto.getContent())
                .build();

        return BoardResponseDto.from(boardRepository.save(board));
    }

    // 게시글 수정
    @Transactional
    public BoardResponseDto updateBoard(Long boardId, BoardRequestDto.Update dto) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 본인(또는 관리자) 소유의 게시글만 수정 가능
        SecurityUtil.checkOwnerOrAdmin(board.getMember().getEmail());

        // null 체크 후 값이 있을 때만 수정
        if (dto.getTitle()   != null) board.setTitle(dto.getTitle());
        if (dto.getContent() != null) board.setContent(dto.getContent());

        // @Transactional 덕분에 save() 없이도 변경사항 자동 반영 (더티 체킹)
        return BoardResponseDto.from(board);
    }

    // 게시글 삭제 (소프트 삭제)
    @Transactional
    public void deleteBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 본인(또는 관리자) 소유의 게시글만 삭제 가능
        SecurityUtil.checkOwnerOrAdmin(board.getMember().getEmail());

        board.setIsActive(false);  // 비활성화 처리
    }
}