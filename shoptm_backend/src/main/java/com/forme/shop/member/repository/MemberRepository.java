package com.forme.shop.member.repository;

import com.forme.shop.member.entity.Member;  // 반드시 우리 Member 클래스 import
import org.springframework.data.jpa.repository.JpaRepository;

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
}