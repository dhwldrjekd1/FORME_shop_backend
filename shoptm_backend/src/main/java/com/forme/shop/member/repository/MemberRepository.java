package com.forme.shop.member.repository;

import com.forme.shop.member.entity.Member;  // 반드시 우리 Member 클래스 import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// JpaRepository 상속만 해도 기본 CRUD 자동 제공
// save, findById, findAll, deleteById 등 직접 구현 안 해도 됨
// <Member, Long> = <엔티티 타입, PK 타입>
public interface MemberRepository extends JpaRepository<Member, Long> {

    // SELECT * FROM users WHERE email = ?
    // 로그인, 이메일 중복 체크 시 사용
    // Optional = 결과 없을 수도 있음 (null 대신 Optional.empty() 반환)
    Optional<Member> findByEmail(String email);

    // SELECT COUNT(*) > 0 FROM users WHERE email = ?
    // 회원가입 시 이메일 중복 여부 확인
    // true = 이미 존재 / false = 사용 가능
    boolean existsByEmail(String email);

    // 정지/탈퇴된 회원 목록 — 서버 기동 시 TokenBlacklistInitializer가 이걸 읽어
    // 메모리 블랙리스트를 복원하는 데 사용 (재배포로 밴 기록이 풀리는 것 방지)
    List<Member> findByIsActiveFalseAndDeactivatedAtIsNotNull();

    // 관리자 회원 검색(이름 또는 이메일에 키워드 포함) — 예전엔 AdminService가 findAll()로
    // 전체 회원을 애플리케이션 메모리에 올린 뒤 Java 스트림으로 걸러냈는데, 회원 수가 늘어날수록
    // 검색 한 번에 전체 테이블을 읽어오는 구조라 확장성이 없었음. 필터링 자체를 DB 쿼리로
    // 넘겨서 실제로 일치하는 행만 가져오도록 변경.
    // LIKE 패턴 특수문자(%, _)를 AdminService에서 미리 이스케이프해 넘기고 여기서 ESCAPE로
    // 그 이스케이프를 해석한다 — 그냥 Spring Data의 Containing 파생 쿼리를 쓰면 keyword 안의
    // '%'/'_'가 원래 문자가 아니라 LIKE 와일드카드로 해석돼(예: 이름에 '_'가 그대로 들어간
    // "a_b"를 검색했는데 "a1b"/"axb" 같은 무관한 회원까지 걸리는 등) 검색 결과가 부정확해진다.
    @Query("SELECT m FROM Member m WHERE m.name LIKE CONCAT('%', :keyword, '%') ESCAPE '\\' " +
           "OR m.email LIKE CONCAT('%', :keyword, '%') ESCAPE '\\'")
    List<Member> searchByNameOrEmail(@Param("keyword") String escapedKeyword);
}