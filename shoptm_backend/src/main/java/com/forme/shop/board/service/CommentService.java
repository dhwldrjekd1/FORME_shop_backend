package com.forme.shop.board.service;

import com.forme.shop.board.dto.CommentRequestDto;
import com.forme.shop.board.dto.CommentResponseDto;
import com.forme.shop.board.entity.Board;
import com.forme.shop.board.entity.Comment;
import com.forme.shop.board.repository.BoardRepository;
import com.forme.shop.board.repository.CommentRepository;
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
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final MemberService memberService;

    // 특정 게시글의 댓글 목록 조회
    // 오래된 순서로 반환 (댓글은 위에서 아래로 시간순)
    public List<CommentResponseDto> getComments(Long boardId) {
        return commentRepository.findByBoardIdAndIsActiveTrueOrderByCreatedAtAsc(boardId)
                .stream()
                .map(CommentResponseDto::from)
                .collect(Collectors.toList());
    }

    // 특정 회원이 작성한 댓글 목록 조회 ("내 댓글" — 작성/수정/삭제와 달리 이 조회에는 소유자
    // 검증이 통째로 빠져 있어서, memberId만 바꿔서 다른 회원의 댓글 목록을 그대로 볼 수 있었음
    public List<CommentResponseDto> getMyComments(Long memberId) {
        memberService.findSelfOrAdminMember(memberId);

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

        // 본인(또는 관리자) 명의로만 댓글 작성 가능
        Member member = memberService.findSelfOrAdminMember(memberId);

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
        Comment comment = findSelfOrAdminComment(commentId);

        comment.setContent(dto.getContent());  // 내용 수정
        return CommentResponseDto.from(comment);
    }

    // 댓글 삭제 (소프트 삭제)
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = findSelfOrAdminComment(commentId);

        comment.setIsActive(false);  // 비활성화 처리
    }

    // "댓글 id로 대상을 찾되, 본인 것이거나 관리자일 때만 결과를 내어준다"를 한 번에 처리한다.
    // MemberService.findSelfOrAdminMember()와 같은 이유(존재 여부 열거 방지) — 대상을 먼저
    // 조회해서 없으면 400, 있는데 내 게 아니면 403을 따로 응답하면, 로그인만 한 상태로 남의
    // commentId를 넣어봤을 때 그 응답 차이만으로 유효한 댓글 id를 하나씩 찾아낼 수 있었다.
    private Comment findSelfOrAdminComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (SecurityUtil.isAdmin()) {
            if (comment == null) {
                throw new IllegalArgumentException("존재하지 않는 댓글입니다.");
            }
            return comment;
        }
        if (comment == null || !SecurityUtil.isSelf(comment.getMember().getEmail())) {
            throw new AccessDeniedException("본인의 정보만 접근할 수 있습니다.");
        }
        return comment;
    }
}