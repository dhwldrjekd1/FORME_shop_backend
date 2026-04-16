package com.forme.shop.member.controller;

import com.forme.shop.member.dto.MemberRequestDto;
import com.forme.shop.member.dto.MemberResponseDto;
import com.forme.shop.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import java.util.List;

@RestController               // @Controller + @ResponseBody: JSON 형태로 응답 반환
@RequestMapping("/api")       // 이 컨트롤러의 모든 URL 앞에 /api 붙음
@RequiredArgsConstructor      // Lombok: final 필드 생성자 주입 자동 처리
public class MemberController {

    private final MemberService memberService;

    // 회원가입
    // POST /api/register
    // @Valid: RequestDto의 유효성 검증 어노테이션 동작시킴 (@NotBlank, @Email 등)
    @PostMapping("/register")
    public ResponseEntity<MemberResponseDto> register(
            @Valid @RequestBody MemberRequestDto.Register dto) {
        return ResponseEntity.ok(memberService.register(dto));
    }
    // 로그인
    // POST /api/login → { token, id, email, name, role, grade }
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody MemberRequestDto.Login dto) {
        return ResponseEntity.ok(memberService.login(dto));
    }


    // 내 정보 조회 (마이페이지)
    // GET /api/members/{id}
    @GetMapping("/members/{id}")
    public ResponseEntity<MemberResponseDto> getMember(
            @PathVariable Long id) {  // URL 경로의 {id} 값을 파라미터로 받음
        return ResponseEntity.ok(memberService.getMember(id));
    }

    // 회원정보 수정
    // PUT /api/members/{id}
    @PutMapping("/members/{id}")
    public ResponseEntity<MemberResponseDto> update(
            @PathVariable Long id,
            @RequestBody MemberRequestDto.Update dto) {  // 요청 Body의 JSON을 DTO로 변환
        return ResponseEntity.ok(memberService.update(id, dto));
    }

    // 회원탈퇴
    // DELETE /api/members/{id}
    // 204 No Content: 처리 성공했지만 반환할 데이터 없음
    @DeleteMapping("/members/{id}")
    public ResponseEntity<Void> withdraw(@PathVariable Long id) {
        memberService.withdraw(id);
        return ResponseEntity.noContent().build();
    }

}