package com.forme.shop.board.service;

import com.forme.shop.board.dto.BoardRequestDto;
import com.forme.shop.board.dto.BoardResponseDto;
import com.forme.shop.board.entity.Board;
import com.forme.shop.board.repository.BoardRepository;
import com.forme.shop.common.security.SecurityUtil;
import com.forme.shop.member.entity.Member;
import com.forme.shop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
        Board board = findSelfOrAdminBoard(boardId);

        // null 체크 후 값이 있을 때만 수정
        if (dto.getTitle()   != null) board.setTitle(dto.getTitle());
        if (dto.getContent() != null) board.setContent(dto.getContent());

        // @Transactional 덕분에 save() 없이도 변경사항 자동 반영 (더티 체킹)
        return BoardResponseDto.from(board);
    }

    // 게시글 삭제 (소프트 삭제)
    @Transactional
    public void deleteBoard(Long boardId) {
        Board board = findSelfOrAdminBoard(boardId);

        board.setIsActive(false);  // 비활성화 처리
    }

    // "게시글 id로 대상을 찾되, 본인 것이거나 관리자일 때만 결과를 내어준다"를 한 번에 처리한다.
    // MemberService.findSelfOrAdminMember()와 같은 이유(존재 여부 열거 방지) — 대상을 먼저
    // 조회해서 없으면 400, 있는데 내 게 아니면 403을 따로 응답하면, 로그인만 한 상태로 남의
    // boardId를 넣어봤을 때 그 응답 차이만으로 유효한 게시글 id를 하나씩 찾아낼 수 있었다.
    private Board findSelfOrAdminBoard(Long boardId) {
        Board board = boardRepository.findById(boardId).orElse(null);
        if (SecurityUtil.isAdmin()) {
            if (board == null) {
                throw new IllegalArgumentException("존재하지 않는 게시글입니다.");
            }
            return board;
        }
        if (board == null || !SecurityUtil.isSelf(board.getMember().getEmail())) {
            throw new AccessDeniedException("본인의 정보만 접근할 수 있습니다.");
        }
        return board;
    }
}