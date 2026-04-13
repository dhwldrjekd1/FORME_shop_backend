package com.forme.shop.config;

import com.forme.shop.member.entity.Member;
import com.forme.shop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 관리자 계정이 없으면 자동 생성
        String adminEmail = "admin@forme.com";
        if (memberRepository.findByEmail(adminEmail).isEmpty()) {
            Member admin = Member.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin1234"))
                    .name("관리자")
                    .role("ROLE_ADMIN")
                    .grade("VIP")
                    .build();
            memberRepository.save(admin);
            log.info("✅ 관리자 계정 생성: {} / admin1234", adminEmail);
        } else {
            log.info("✅ 관리자 계정 이미 존재: {}", adminEmail);
        }
    }
}
