package com.forme.shop.member.service;

import com.forme.shop.common.security.SecurityUtil;
import com.forme.shop.config.jwt.JwtUtil;
import com.forme.shop.config.jwt.TokenBlacklistService;
import com.forme.shop.member.dto.MemberRequestDto;
import com.forme.shop.member.dto.MemberResponseDto;
import com.forme.shop.member.entity.Member;  // 반드시 우리 Member 클래스 import
import com.forme.shop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service                          // 스프링 빈으로 등록, 비즈니스 로직 담당
@RequiredArgsConstructor          // Lombok: final 필드를 생성자 주입으로 자동 처리
@Transactional(readOnly = true)   // 기본적으로 읽기 전용 트랜잭션 (조회 성능 최적화)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;  // BCrypt 암호화

    // 회원가입
    // @Transactional: 데이터 변경이 있으므로 쓰기 트랜잭션 적용
    @Transactional
    public MemberResponseDto register(MemberRequestDto.Register dto) {

        // 이메일 중복 체크
        if (memberRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 빌더 패턴으로 Member 엔티티 생성
        // 비밀번호는 BCrypt로 암호화해서 저장
        Member member = Member.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .height(dto.getHeight())
                .weight(dto.getWeight())
                .fit(dto.getFit())
                .build();

        // DB에 저장 후 DTO로 변환해서 반환
        return MemberResponseDto.from(memberRepository.save(member));
    }

    // 회원 단건 조회 (마이페이지)
    public MemberResponseDto getMember(Long id) {

        // orElseThrow: 회원이 없으면 예외 발생
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 본인(또는 관리자)만 조회 가능 — 로그인한 아무 회원이나 id만 바꿔서 조회하는 것 방지
        SecurityUtil.checkOwnerOrAdmin(member.getEmail());

        return MemberResponseDto.from(member);
    }

    private final JwtUtil jwtUtil;  // 필드에 추가
    private final TokenBlacklistService tokenBlacklistService;

    // 로그인
    // 응답에 token + 회원 식별/표시 정보 포함 (프론트가 마이페이지·Q&A 작성 등에 사용)
    @Transactional(readOnly = true)
    public Map<String, Object> login(MemberRequestDto.Login dto) {

        // 이메일로 회원 조회
        Member member = memberRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 틀렸습니다."));

        // 비밀번호 검증을 계정 활성 여부보다 먼저 확인한다 — 순서가 바뀌어 있으면, 비밀번호를
        // 전혀 몰라도 아무 값이나 넣어 "사용할 수 없는 계정입니다"가 뜨는지만 보고 그 이메일이
        // 정지/탈퇴된 계정인지 알아낼 수 있는 계정 상태 열거(enumeration) 통로가 생긴다.
        // 비밀번호가 맞아야만(=그 계정의 진짜 소유자여야만) 활성 여부를 알려주도록 순서를 바꿈.
        if (!passwordEncoder.matches(dto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 틀렸습니다.");
        }

        // 탈퇴/강퇴 회원 체크
        if (!member.getIsActive()) {
            throw new IllegalArgumentException("사용할 수 없는 계정입니다.");
        }

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(member.getEmail(), member.getRole());

        // 토큰 + 회원 정보 같이 반환
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("token", token);
        result.put("id", member.getId());
        result.put("email", member.getEmail());
        result.put("name", member.getName());
        result.put("role", member.getRole());
        result.put("grade", member.getGrade());
        result.put("height", member.getHeight());
        result.put("weight", member.getWeight());
        result.put("fit", member.getFit());
        return result;
    }

    // 회원정보 수정
    @Transactional
    public MemberResponseDto update(Long id, MemberRequestDto.Update dto) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 본인(또는 관리자)만 수정 가능
        SecurityUtil.checkOwnerOrAdmin(member.getEmail());

        // null 체크 후 값이 있을 때만 수정 (부분 수정 가능)
        if (dto.getName()     != null) member.setName(dto.getName());
        if (dto.getPhone()    != null) member.setPhone(dto.getPhone());
        if (dto.getAddress()  != null) member.setAddress(dto.getAddress());
        if (dto.getPassword() != null) {
            // 자기 자신의 비밀번호를 바꾸는 경우(대상 계정 == 현재 로그인 계정)엔 현재 비밀번호
            // 확인을 요구한다 — 안 그러면 세션이 탈취된 상태(XSS, 방치된 브라우저 등)에서 원래
            // 비밀번호를 몰라도 새 비밀번호로 덮어써서 진짜 주인을 계정에서 영구히 쫓아낼 수
            // 있었음. 이 판단은 role이 아니라 "대상이 본인 계정인지"로 해야 한다 — role로만
            // 판단하면(!isAdmin()) 관리자가 '자기 자신의' 비밀번호를 바꿀 때도 확인이 빠져서,
            // 정작 이 기능이 막으려던 것과 똑같이 관리자 세션이 탈취된 경우에 그대로 뚫린다.
            // 관리자가 '다른' 회원의 비밀번호를 대신 바꿔주는 경우(분실 계정 지원 등)만
            // 확인에서 제외한다 — 그때는 애초에 현재 비밀번호를 모르는 게 정상 시나리오이므로.
            if (SecurityUtil.isSelf(member.getEmail())) {
                if (dto.getCurrentPassword() == null
                        || !passwordEncoder.matches(dto.getCurrentPassword(), member.getPassword())) {
                    throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
                }
            }
            member.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getHeight()   != null) member.setHeight(dto.getHeight());
        if (dto.getWeight()   != null) member.setWeight(dto.getWeight());
        if (dto.getFit()      != null) member.setFit(dto.getFit());

        return MemberResponseDto.from(member);
    }

    // 회원탈퇴 (소프트 삭제)
    // 실제 DB에서 삭제하지 않고 isActive = false 로 변경
    @Transactional
    public void withdraw(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 본인(또는 관리자)만 탈퇴 처리 가능
        SecurityUtil.checkOwnerOrAdmin(member.getEmail());

        member.setIsActive(false);  // 비활성화 처리
        member.setDeactivatedAt(LocalDateTime.now());
        // 이미 발급된 토큰이 남아있으면(다른 기기 로그인 등) 탈퇴 후에도 만료 전까지
        // 계속 인증되던 문제 — 이 회원의 모든 토큰을 즉시 무효화
        tokenBlacklistService.revokeAllForEmail(member.getEmail());
    }

    // 로그아웃 — 지금 쓰던 토큰의 jti를 블랙리스트에 등록해서 만료 전이라도 즉시 무효화
    public void logout(String token) {
        if (token == null || !jwtUtil.validateToken(token)) return;  // 없거나 이미 무효한 토큰이면 등록할 필요 없음

        String jti = jwtUtil.getJti(token);
        java.time.Instant expiresAt = jwtUtil.getExpiration(token).toInstant();
        tokenBlacklistService.revoke(jti, expiresAt);
    }

    // 관리자 - 전체 회원 목록 조회
    public List<MemberResponseDto> getAllMembers() {
        return memberRepository.findAll()
                .stream()
                .map(MemberResponseDto::from)   // 각 Member 엔티티를 DTO로 변환
                .collect(Collectors.toList());
    }

    // 관리자 - 회원 강퇴 (소프트 삭제, 탈퇴와 동일한 처리)
    @Transactional
    public void banMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        member.setIsActive(false);  // 비활성화 처리
        member.setDeactivatedAt(LocalDateTime.now());
        // 강퇴는 특히 "지금 당장 접근을 끊어야 하는" 상황이므로, 남은 세션이
        // 토큰 만료 시각(최대 24시간)까지 계속 유효하게 남아있으면 안 됨
        tokenBlacklistService.revokeAllForEmail(member.getEmail());
    }

    // 관리자 - 회원 등급 변경 (BRONZE / SILVER / GOLD)
    @Transactional
    public void changeGrade(Long id, String grade) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        member.setGrade(grade);
    }
}