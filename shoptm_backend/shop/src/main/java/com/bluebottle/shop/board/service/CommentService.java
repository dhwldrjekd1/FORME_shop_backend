package com.bluebottle.shop.board.service;

import com.bluebottle.shop.board.dto.CommentRequestDto;
import com.bluebottle.shop.board.dto.CommentResponseDto;
import com.bluebottle.shop.board.entity.Board;
import com.bluebottle.shop.board.entity.Comment;
import com.bluebottle.shop.board.repository.BoardRepository;
import com.bluebottle.shop.board.repository.CommentRepository;
import com.bluebottle.shop.member.entity.Member;
import com.bluebottle.shop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    // 특정 게시글의 댓글 목록 조회
    // 오래된 순서로 반환 (댓글은 위에서 아래로 시간순)
    public List<CommentResponseDto> getComments(Long boardId) {
        return commentRepository.findByBoardIdAndIsActiveTrueOrderByCreatedAtAsc(boardId)
                .stream()
                .map(CommentResponseDto::from)
                .collect(Collectors.toList());
    }

    // 특정 회원이 작성한 댓글 목록 조회
    public List<CommentResponseDto> getMyComments(Long memberId) {
        return commentRepository.findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(memberId)
                .stream()
                .map(CommentResponseDto::from)
                .collect(Collectors.toList());
    }

    // 댓글 작성
    @Transactional
    public CommentResponseDto createComment(Long boardId, Long memberId,
                                            CommentRequestDto.Create dto) {
        // 게시글 존재 여부 확인
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 삭제된 게시글에는 댓글 작성 불가
        if (!board.getIsActive()) {
            throw new IllegalArgumentException("삭제된 게시글에는 댓글을 작성할 수 없습니다.");
        }

        // 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Comment comment = Comment.builder()
                .board(board)
                .member(member)
                .content(dto.getContent())
                .build();

        return CommentResponseDto.from(commentRepository.save(comment));
    }

    // 댓글 수정
    @Transactional
    public CommentResponseDto updateComment(Long commentId, CommentRequestDto.Update dto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        comment.setContent(dto.getContent());  // 내용 수정
        return CommentResponseDto.from(comment);
    }

    // 댓글 삭제 (소프트 삭제)
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
        comment.setIsActive(false);  // 비활성화 처리
    }
}