package com.forme.shop.settings.service;

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

    @Transactional
    public void setValue(String key, String value) {
        SiteSetting setting = repository.findByKey(key)
                .orElse(SiteSetting.builder().key(key).build());
        setting.setValue(value);
        repository.save(setting);
    }
}
