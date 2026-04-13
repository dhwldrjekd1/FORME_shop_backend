package com.forme.shop.config;

import com.forme.shop.category.entity.Category;
import com.forme.shop.category.repository.CategoryRepository;
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
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 관리자 계정
        String adminEmail = "admin@forme.com";
        if (memberRepository.findByEmail(adminEmail).isEmpty()) {
            memberRepository.save(Member.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin1234"))
                    .name("관리자")
                    .role("ROLE_ADMIN")
                    .grade("VIP")
                    .build());
            log.info("✅ 관리자 계정 생성: {} / admin1234", adminEmail);
        }

        // 기본 카테고리
        String[][] cats = {
            {"상의", "티셔츠, 셔츠, 니트 등"},
            {"하의", "팬츠, 데님, 쇼츠 등"},
            {"아우터", "자켓, 코트, 점퍼 등"},
            {"액세서리", "모자, 가방, 벨트 등"},
        };
        for (int i = 0; i < cats.length; i++) {
            String name = cats[i][0];
            String desc = cats[i][1];
            if (categoryRepository.findAll().stream().noneMatch(c -> c.getName().equals(name))) {
                categoryRepository.save(Category.builder()
                        .name(name)
                        .description(desc)
                        .sortOrder(i + 1)
                        .build());
                log.info("✅ 카테고리 생성: {}", name);
            }
        }
    }
}
