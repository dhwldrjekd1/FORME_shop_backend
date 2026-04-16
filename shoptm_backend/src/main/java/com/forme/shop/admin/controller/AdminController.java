package com.forme.shop.admin.controller;

import com.forme.shop.admin.dto.DashboardResponseDto;
import com.forme.shop.admin.service.AdminService;
import com.forme.shop.member.dto.MemberResponseDto;
import com.forme.shop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController               // @Controller + @ResponseBody: JSON 형태로 응답 반환
@RequestMapping("/api/admin") // 이 컨트롤러의 모든 URL 앞에 /api/admin 붙음
@RequiredArgsConstructor      // Lombok: final 필드 생성자 주입 자동 처리
public class AdminController {

    private final AdminService adminService;
    private final MemberService memberService;

    // 관리자 대시보드 통계 조회
    // GET /api/admin/dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponseDto> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    // 관리자 - 전체 회원 목록
    // GET /api/admin/members
    @GetMapping("/members")
    public ResponseEntity<List<MemberResponseDto>> getAllMembers() {
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    // 관리자 - 회원 검색
    // GET /api/admin/members/search?keyword=홍길동
    @GetMapping("/members/search")
    public ResponseEntity<List<MemberResponseDto>> searchMembers(
            @RequestParam String keyword) {
        return ResponseEntity.ok(
                adminService.searchMembers(keyword)
                        .stream()
                        .map(MemberResponseDto::from)
                        .collect(Collectors.toList())
        );
    }

    // 관리자 - 회원 강퇴
    // PATCH /api/admin/members/{id}/ban
    @PatchMapping("/members/{id}/ban")
    public ResponseEntity<Void> banMember(@PathVariable Long id) {
        memberService.banMember(id);
        return ResponseEntity.noContent().build();
    }

    // 관리자 - 회원 등급 변경
    // PATCH /api/admin/members/{id}/grade?grade=SILVER
    @PatchMapping("/members/{id}/grade")
    public ResponseEntity<Void> changeGrade(
            @PathVariable Long id,
            @RequestParam String grade) {
        memberService.changeGrade(id, grade);
        return ResponseEntity.noContent().build();
    }
}