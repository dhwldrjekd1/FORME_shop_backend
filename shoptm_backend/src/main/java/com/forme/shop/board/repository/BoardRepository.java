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

    // 제목으로 게시글 검색 — MemberRepository.searchByNameOrEmail과 동일한 이유로 이스케이프한
    // 커스텀 쿼리를 쓴다. 예전엔 Spring Data의 Containing 파생 쿼리를 그대로 썼는데, keyword
    // 안의 '%'/'_'가 원래 문자가 아니라 LIKE 와일드카드로 해석돼(예: 제목에 '_'가 그대로 들어간
    // "a_b"를 검색했는데 "axb" 같은 무관한 게시글까지 걸림) 검색 결과가 부정확해졌음
    // (BoardService.searchBoards가 이스케이프해서 넘김).
    @Query("SELECT b FROM Board b WHERE b.title LIKE CONCAT('%', :keyword, '%') ESCAPE '\\' " +
           "AND b.isActive = true ORDER BY b.createdAt DESC")
    List<Board> searchByTitle(@Param("keyword") String escapedKeyword);

    // UPDATE boards SET views = views + 1 WHERE id = ?
    // 조회수 1 증가 (게시글 상세 조회 시 호출)
    // clearAutomatically=true: 이 벌크 UPDATE는 영속성 컨텍스트를 거치지 않고 DB를 직접 갱신하므로,
    // 호출 전에 이미 로드해둔 Board 엔티티가 있다면 그 인스턴스의 views는 갱신되지 않은 값 그대로
    // 남는다. 컨텍스트를 비워서, 이후 응답 조립을 위해 다시 조회할 때 반드시 최신 값을 새로
    // 읽어오도록 강제한다(BoardService.getBoard() 참고 — 반드시 이 호출 뒤에 재조회할 것).
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Board b SET b.views = b.views + 1 WHERE b.id = :id")
    void incrementViews(@Param("id") Long id);
}