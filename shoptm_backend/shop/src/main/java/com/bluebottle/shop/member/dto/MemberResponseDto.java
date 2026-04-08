package com.bluebottle.shop.member.dto;

import com.bluebottle.shop.member.entity.Member;  // 반드시 우리 Member 클래스 import
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 클라이언트에게 회원 정보를 응답할 때 사용하는 DTO
// Entity를 직접 반환하면 password 등 민감한 정보가 노출될 수 있어서 DTO로 변환 후 반환
@Getter
@Builder
public class MemberResponseDto {

    private Long id;
    private String email;
    private String name;
    private String phone;
    private String address;
    private String role;
    private String grade;
    private Boolean isActive;
    private LocalDateTime createdAt;
    // password는 보안상 응답에 포함하지 않음

    // Member 엔티티를 MemberResponseDto로 변환하는 정적 메서드
    // 서비스에서 return MemberResponseDto.from(member) 형태로 사용
    public static MemberResponseDto from(Member member) {
        return MemberResponseDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .phone(member.getPhone())
                .address(member.getAddress())
                .role(member.getRole())
                .grade(member.getGrade())
                .isActive(member.getIsActive())
                .createdAt(member.getCreatedAt())
                .build();
    }
}