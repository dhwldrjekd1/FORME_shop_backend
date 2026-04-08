package com.bluebottle.shop.member.service;

import com.bluebottle.shop.config.jwt.JwtUtil;
import com.bluebottle.shop.member.dto.MemberRequestDto;
import com.bluebottle.shop.member.dto.MemberResponseDto;
import com.bluebottle.shop.member.entity.Member;  // 반드시 우리 Member 클래스 import
import com.bluebottle.shop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
                .build();

        // DB에 저장 후 DTO로 변환해서 반환
        return MemberResponseDto.from(memberRepository.save(member));
    }

    // 회원 단건 조회 (마이페이지)
    public MemberResponseDto getMember(Long id) {

        // orElseThrow: 회원이 없으면 예외 발생
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        return MemberResponseDto.from(member);
    }

    private final JwtUtil jwtUtil;  // 필드에 추가

    // 로그인
    @Transactional(readOnly = true)
    public String login(MemberRequestDto.Login dto) {

        // 이메일로 회원 조회
        Member member = memberRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 틀렸습니다."));

        // 탈퇴/강퇴 회원 체크
        if (!member.getIsActive()) {
            throw new IllegalArgumentException("사용할 수 없는 계정입니다.");
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(dto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 틀렸습니다.");
        }

        // JWT 토큰 생성 후 반환
        return jwtUtil.generateToken(member.getEmail(), member.getRole());
    }

    // 회원정보 수정
    @Transactional
    public MemberResponseDto update(Long id, MemberRequestDto.Update dto) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // null 체크 후 값이 있을 때만 수정 (부분 수정 가능)
        if (dto.getName()     != null) member.setName(dto.getName());
        if (dto.getPhone()    != null) member.setPhone(dto.getPhone());
        if (dto.getAddress()  != null) member.setAddress(dto.getAddress());
        if (dto.getPassword() != null) {
            // 비밀번호 변경 시에도 BCrypt로 암호화
            member.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        // @Transactional 덕분에 save() 없이도 변경사항 자동 반영 (더티 체킹)
        return MemberResponseDto.from(member);
    }

    // 회원탈퇴 (소프트 삭제)
    // 실제 DB에서 삭제하지 않고 isActive = false 로 변경
    @Transactional
    public void withdraw(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        member.setIsActive(false);  // 비활성화 처리
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
    }

    // 관리자 - 회원 등급 변경 (BRONZE / SILVER / GOLD)
    @Transactional
    public void changeGrade(Long id, String grade) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        member.setGrade(grade);
    }
}