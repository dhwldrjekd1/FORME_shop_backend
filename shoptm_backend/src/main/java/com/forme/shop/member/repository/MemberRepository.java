package com.forme.shop.member.repository;

import com.forme.shop.member.entity.Member;  // 반드시 우리 Member 클래스 import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

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

    // grade 한 컬럼만 원자적으로 바꾼다 (OrderService.updateMemberGrade 전용).
    // 거기서 로드해 들고 있는 member는 그 사이 decreaseStockIfAvailable()의
    // clearAutomatically=true로 이미 detach된 상태라, memberRepository.save(member)(merge)를
    // 쓰면 이 member 변수가 로드된 시점 이후 다른 요청(예: 회원정보 수정, 비밀번호 변경)이
    // 커밋한 다른 모든 컬럼까지 그 스냅샷 값으로 통째로 덮어써버린다. grade만 UPDATE해서
    // 그 문제를 피한다.
    // WHERE에 gradeManual = false도 넣어 "관리자가 수동 지정한 등급은 자동 재계산이 건드리지
    // 않는다"는 조건 자체를 원자적 UPDATE에 못박는다 — 호출부가 들고 있는 member.getGradeManual()
    // 값으로 미리 판단하면, 그 member를 읽어온 시점과 지금 사이에 관리자가 막 changeGrade()를
    // 호출해 gradeManual을 true로 바꿨어도 그 변경을 못 보고 자동 재계산을 그대로 진행해버리는
    // 레이스가 생긴다(교차검증에서 발견) — 조건을 DB 쪽에 두면 항상 최신 값으로 판단된다.
    @Modifying
    @Query("UPDATE Member m SET m.grade = :grade WHERE m.id = :id AND m.gradeManual = false")
    int updateGradeIfNotManual(@Param("id") Long id, @Param("grade") String grade);

    // OrderService.updateMemberGrade 전용: 같은 회원이 거의 동시에 두 개의 주문을 완료하면,
    // 각 트랜잭션이 READ COMMITTED 기준으로 서로의(아직 커밋 안 된) 주문을 못 보고 누적
    // 구매액을 계산해서, 둘을 합치면 등급 상향 임계값을 넘는데도 양쪽 다 상향을 건너뛰는
    // 일이 있었다(다음 주문에서 결국 정정되긴 하지만 그만큼 늦어짐 — 교차검증에서 발견).
    // 이 회원 row에 PESSIMISTIC_WRITE 락을 걸어, 두 번째 트랜잭션이 첫 번째가 커밋할 때까지
    // 기다리게 만든다 — 그 뒤에야 누적 구매액을 다시 읽으므로(READ COMMITTED는 문장마다 새로
    // 커밋된 데이터를 보므로) 그 시점에는 첫 번째 주문이 이미 반영된 금액을 보게 된다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);
}