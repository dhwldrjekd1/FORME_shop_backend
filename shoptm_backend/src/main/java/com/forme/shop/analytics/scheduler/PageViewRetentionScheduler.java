package com.forme.shop.analytics.scheduler;

import com.forme.shop.analytics.repository.PageViewRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// /api/analytics/track으로 쌓이는 page_views는 삭제되는 계기가 전혀 없어 영구히 쌓이기만
// 했다(다른 방향에서 버그를 찾다가 발견 — AnalyticsRateLimitFilter로 비정상적인 유입 속도는
// 막았지만, 정상적인 트래픽만으로도 테이블이 끝없이 커지는 문제는 별개). 관리자 통계
// (AnalyticsService)는 전부 "최근 추이"를 보는 용도라 오래된 원본 기록을 영구 보관할 이유가
// 없어, 일정 기간이 지난 기록은 주기적으로 삭제해 테이블 크기를 관리한다.
@Component
@RequiredArgsConstructor
public class PageViewRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(PageViewRetentionScheduler.class);

    // 관리자 화면에 "최근 추이"를 넉넉히 보여주면서도 무한정 쌓이지는 않도록 하는 균형점으로
    // 180일(6개월)을 잡음 — 더 짧게 잡을 특별한 요구사항은 아직 없음.
    static final int RETENTION_DAYS = 180;

    private final PageViewRepository pageViewRepository;

    // 매일 한 번씩 정리 (서버 기동 5분 뒤부터) — OrderExpiryScheduler처럼 자주 돌 필요는 없는
    // 정리 작업이라 주기를 더 길게 둠.
    @Scheduled(fixedDelay = 24L * 60 * 60 * 1000, initialDelay = 5 * 60 * 1000L)
    @Transactional
    public void deleteExpiredPageViews() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int deleted = pageViewRepository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("보존기간({}일)이 지난 페이지뷰 기록 {}건 삭제함", RETENTION_DAYS, deleted);
        }
    }
}
