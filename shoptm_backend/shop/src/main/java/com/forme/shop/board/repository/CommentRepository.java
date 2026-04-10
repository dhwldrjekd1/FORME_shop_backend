package com.forme.shop.board.repository;

import com.forme.shop.board.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // SELECT * FROM comments WHERE board_id = ? AND is_active = true ORDER BY created_at ASC
    // 특정 게시글의 댓글 목록 오래된순 조회 (댓글은 오래된 것부터 보여줌)
    List<Comment> findByBoardIdAndIsActiveTrueOrderByCreatedAtAsc(Long boardId);

    // SELECT * FROM comments WHERE member_id = ? AND is_active = true ORDER BY created_at DESC
    // 특정 회원이 작성한 댓글 목록 최신순 조회
    List<Comment> findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(Long memberId);
}