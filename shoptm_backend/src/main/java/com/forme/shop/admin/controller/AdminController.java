package com.forme.shop.admin.controller;

import com.forme.shop.admin.dto.DashboardResponseDto;
import com.forme.shop.admin.service.AdminService;
import com.forme.shop.member.dto.MemberRequestDto;
import com.forme.shop.member.dto.MemberResponseDto;
import com.forme.shop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // 관리자 - 전체 회원 목록. 회원 수가 늘어나면 한 번에 다 돌려주는 게 부담이라
    // 페이지 단위로 끊어서 내려준다(교차검증에서 발견 — 관리자 화면 전체에 공통된 문제).
    // 정렬 기준을 명시(id desc)해서, DB가 보장 안 해주는 순서에 화면이 흔들리는 것도 막음.
    // GET /api/admin/members?page=0&size=20
    @GetMapping("/members")
    public ResponseEntity<Page<MemberResponseDto>> getAllMembers(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(memberService.getAllMembers(pageable));
    }

    // 관리자 - 회원 검색
    // GET /api/admin/members/search?keyword=홍길동&page=0&size=20
    @GetMapping("/members/search")
    public ResponseEntity<Page<MemberResponseDto>> searchMembers(
            @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.searchMembers(keyword, pageable).map(MemberResponseDto::from));
    }

    // 관리자 - 회원 강퇴
    // PATCH /api/admin/members/{id}/ban
    // body는 선택 — 다른 회원을 강퇴할 땐 안 보내도 됨(MemberService 참고)
    @PatchMapping("/members/{id}/ban")
    public ResponseEntity<Void> banMember(
            @PathVariable Long id,
            @RequestBody(required = false) MemberRequestDto.Ban dto) {
        memberService.banMember(id, dto != null ? dto.getCurrentPassword() : null);
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