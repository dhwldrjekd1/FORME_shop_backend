package com.forme.shop.settings.service;

import com.forme.shop.common.security.SecurityUtil;
import com.forme.shop.settings.entity.SiteSetting;
import com.forme.shop.settings.repository.SiteSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteSettingService {

    private final SiteSettingRepository repository;

    public Optional<String> getValue(String key) {
        return repository.findByKey(key).map(SiteSetting::getValue);
    }

    // setting_key 컬럼 길이(SiteSetting.key, 100)와 맞춤 — 없으면 DB가 거부하는
    // DataIntegrityViolationException이 그대로 500으로 노출됨. 이 메서드는 DTO가 아니라
    // URL 경로 변수를 그대로 받아서, Category/FAQ/Board/Qna/Member DTO에서 이미 고쳤던 것과
    // 같은 종류의 문제가 여기만 @Size 검증 없이 남아있었다(다른 방향에서 버그를 찾다가 발견).
    // 관리자 전용 입력이라 심각도는 낮지만(오타 정도), 같은 종류의 500 노출은 막는 게 맞음.
    private static final int MAX_KEY_LENGTH = 100;

    @Transactional
    public void setValue(String key, String value) {
        SecurityUtil.requireAdmin();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("설정 키를 입력해주세요.");
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("설정 키는 " + MAX_KEY_LENGTH + "자 이하여야 합니다.");
        }
        repository.upsertSetting(key, value);
    }
}
