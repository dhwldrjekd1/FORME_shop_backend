package com.forme.shop.config.jwt;

import com.forme.shop.member.entity.Member;
import com.forme.shop.member.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TokenBlacklistService의 회원 단위 전체무효화(정지/탈퇴)는 메모리에만 있어서, 서버가
// 재시작될 때마다 비어버리면 이미 강퇴된 회원의 남은 토큰이 다시 유효해지는 문제가 있었음.
// 기동 시 DB에서 비활성 회원(isActive=false, deactivatedAt 있음)을 읽어와 메모리에
// 다시 채워 넣어서, 재배포해도 밴 기록이 풀리지 않도록 함.
//
// CommandLineRunner가 아니라 @PostConstruct를 쓴 이유: CommandLineRunner는 내장 톰캣이 이미
// 요청을 받기 시작한 뒤에(ApplicationContext.refresh() 완료 후) 실행되지만, @PostConstruct는
// 빈 초기화 단계(톰캣이 열리기 전)에 실행돼서 복원이 끝나기 전에 요청이 들어올 여지를 최대한
// 줄여줌 — 다만 Spring Boot 내장 웹서버 구조상 이 창을 완전히 0으로 만들 수는 없고(이 프로젝트
// 트래픽 규모에서는 사실상 무시 가능한 수준), 정확히 0으로 만들려면 서버 시작 자체를 지연시키는
// 더 큰 구조 변경이 필요해 지금 범위에서는 하지 않음.
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistInitializer {

    private final MemberRepository memberRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @PostConstruct
    public void restoreRevocations() {
        List<Member> deactivated = memberRepository.findByIsActiveFalseAndDeactivatedAtIsNotNull();
        if (deactivated.isEmpty()) return;

        Map<String, Instant> cutoffsByEmail = deactivated.stream()
                .collect(Collectors.toMap(
                        Member::getEmail,
                        m -> m.getDeactivatedAt().atZone(ZoneId.systemDefault()).toInstant()
                ));
        tokenBlacklistService.restoreRevocations(cutoffsByEmail);
        log.info("✅ 정지/탈퇴 회원 {}명의 토큰 무효화 상태를 복원했습니다.", deactivated.size());
    }
}
