package com.forme.shop.board.service;

import com.forme.shop.board.dto.BoardRequestDto;
import com.forme.shop.board.dto.BoardResponseDto;
import com.forme.shop.board.entity.Board;
import com.forme.shop.board.repository.BoardRepository;
import com.forme.shop.member.entity.Member;
import com.forme.shop.member.repository.MemberRepository;
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
    private final MemberRepository memberRepository;

    // 전체 게시글 목록 조회
    // 삭제되지 않은 게시글만 최신순으로 반환
    public List<BoardResponseDto> getAllBoards() {
        return boardRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(BoardResponseDto::from)
                .collect(Collectors.toList());
    }

    // 게시글 단건 조회 + 조회수 증가
    // @Transactional: 조회수 증가는 DB 변경이므로 쓰기 트랜잭션 필요
    @Transactional
    public BoardResponseDto getBoard(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 삭제된 게시글 접근 차단
        if (!board.getIsActive()) {
            throw new IllegalArgumentException("삭제된 게시글입니다.");
        }

        // 조회수 1 증가
        boardRepository.incrementViews(id);
        return BoardResponseDto.from(board);
    }

    // 특정 회원의 게시글 목록 조회
    public List<BoardResponseDto> getMyBoards(Long memberId) {
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
        // 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

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
        board.setIsActive(false);  // 비활성화 처리
    }
}