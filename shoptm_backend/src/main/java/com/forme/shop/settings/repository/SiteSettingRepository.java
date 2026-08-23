package com.forme.shop.settings.repository;

import com.forme.shop.settings.entity.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SiteSettingRepository extends JpaRepository<SiteSetting, String> {
    Optional<SiteSetting> findByKey(String key);

    // 있으면 값을 갱신하고 없으면 새로 만드는 원자적 upsert. "조회 후 없으면 새 엔티티로 저장"
    // 방식은 아직 없는 같은 키를 동시에 두 번 저장하면(예: 관리자 두 명이 거의 동시에 설정 저장)
    // 둘 다 "없음"으로 보고 둘 다 새로 만들려다 setting_key PK 위반으로 하나는 500이 남 —
    // 장바구니/찜/리뷰와 같은 이유로 원자적 upsert로 교체.
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO site_settings (setting_key, value, updated_at) " +
            "VALUES (:key, :value, now()) " +
            "ON CONFLICT (setting_key) DO UPDATE SET value = :value, updated_at = now()",
            nativeQuery = true)
    void upsertSetting(@Param("key") String key, @Param("value") String value);
}
