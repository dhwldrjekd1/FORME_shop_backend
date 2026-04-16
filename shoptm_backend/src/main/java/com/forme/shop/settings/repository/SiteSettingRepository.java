package com.forme.shop.settings.repository;

import com.forme.shop.settings.entity.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SiteSettingRepository extends JpaRepository<SiteSetting, String> {
    Optional<SiteSetting> findByKey(String key);
}
