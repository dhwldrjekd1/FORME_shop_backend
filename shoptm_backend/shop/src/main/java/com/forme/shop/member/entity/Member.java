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

    @CreationTimestamp                           // INSERT 시 자동으로 현재 시간 저장
    @Column(nullable = false, updatable = false) // 최초 저장 후 수정 불가
    private LocalDateTime createdAt;

    @UpdateTimestamp                             // UPDATE 시 자동으로 현재 시간 갱신
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}