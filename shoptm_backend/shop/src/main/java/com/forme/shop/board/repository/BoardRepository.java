package com.forme.shop.board.repository;

import com.forme.shop.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    // SELECT * FROM boards WHERE is_active = true ORDER BY created_at DESC
    // 삭제되지 않은 게시글 최신순 조회
    List<Board> findByIsActiveTrueOrderByCreatedAtDesc();

    // SELECT * FROM boards WHERE member_id = ? AND is_active = true ORDER BY created_at DESC
    // 특정 회원이 작성한 게시글 최신순 조회
    List<Board> findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(Long memberId);

    // SELECT * FROM boards WHERE title LIKE %keyword% AND is_active = true
    // 제목으로 게시글 검색
    List<Board> findByTitleContainingAndIsActiveTrueOrderByCreatedAtDesc(String keyword);

    // UPDATE boards SET views = views + 1 WHERE id = ?
    // 조회수 1 증가 (게시글 상세 조회 시 호출)
    @Modifying
    @Query("UPDATE Board b SET b.views = b.views + 1 WHERE b.id = :id")
    void incrementViews(@Param("id") Long id);
}