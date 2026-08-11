package com.forme.shop.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * 회원 엔티티
 * - Spring Security 기반 인증/인가 처리
 * - 소프트 삭제: is_active = false 로 탈퇴/강퇴 처리 (DB에서 실제 삭제 안 함)
 * - 테이블명: member
 */
@Entity
@Table(name = "member")
@Getter @Setter
@NoArgsConstructor               // JPA 기본 생성자 필수
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // BIGSERIAL 자동 증가
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;          // 로그인 아이디, 중복 불가

    @Column(nullable = false, length = 255)
    private String password;
    // BCryptPasswordEncoder 로 암호화해서 저장
    // 절대 평문으로 저장하지 않음

    @Column(nullable = false, length = 50)
    private String name;           // 회원 실명

    @Column(length = 20)
    private String phone;          // 전화번호 (선택 입력)

    @Column(length = 255)
    private String address;        // 주소 (선택 입력)

    @Column
    private Double height;         // 키 (cm)

    @Column
    private Double weight;         // 몸무게 (kg)

    @Column(length = 20)
    private String fit;            // 선호 핏 (slim, standard, wide)

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String role = "ROLE_USER";
    // 권한 구분
    // ROLE_USER  = 일반 회원 (기본값)
    // ROLE_ADMIN = 관리자
    // Spring Security 에서 hasAuthority("ROLE_ADMIN") 으로 관리자 체크

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String grade = "BRONZE";
    // 회원 등급 (관리자가 수동 변경)
    // BRONZE(기본) → SILVER → GOLD → VIP

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;
    // true  = 정상 활성 회원
    // false = 탈퇴 or 강퇴된 회원 (소프트 삭제)
    // 로그인 시 isActive = false 이면 로그인 거부

    @Column
    private LocalDateTime deactivatedAt;
    // 탈퇴/강퇴 처리된 시각 (활성 회원은 NULL). 이 시각 이전에 발급된 JWT는 전부 무효로 취급해서
    // 정지/탈퇴 후에도 만료 전까지 로그인 상태가 유지되던 문제를 막는 기준값으로 씀
    // (TokenBlacklistService — 서버가 재시작돼도 이 값은 DB에 남아있어야 하므로 updatedAt처럼
    // 다른 이유로도 계속 갱신되는 컬럼이 아니라 별도 컬럼으로 둠)

    @CreationTimestamp                           // INSERT 시 자동으로 현재 시간 저장
    @Column(nullable = false, updatable = false) // 최초 저장 후 수정 불가
    private LocalDateTime createdAt;

    @UpdateTimestamp                             // UPDATE 시 자동으로 현재 시간 갱신
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}