package com.forme.shop.config;

import com.forme.shop.member.entity.Member;
import com.forme.shop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 하드코딩된 기본 비밀번호("1234") 대신 배포 환경마다 다른 값을 강제하기 위해 필수 환경변수로 주입
    @Value("${admin.init-password}")
    private String adminInitPassword;

    @Override
    public void run(String... args) {
        // 관리자 계정이 없으면 자동 생성
        String adminEmail = "admin@forme.com";
        if (memberRepository.findByEmail(adminEmail).isEmpty()) {
            Member admin = Member.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminInitPassword))
                    .name("관리자")
                    .role("ROLE_ADMIN")
                    .grade("VIP")
                    .build();
            memberRepository.save(admin);
            log.info("✅ 관리자 계정 생성: {}", adminEmail);
        } else {
            log.info("✅ 관리자 계정 이미 존재: {}", adminEmail);
        }
    }
}
