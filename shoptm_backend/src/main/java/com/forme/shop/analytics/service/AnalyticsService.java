package com.forme.shop.analytics.service;

import com.forme.shop.analytics.dto.PageViewRequest;
import com.forme.shop.analytics.entity.PageView;
import com.forme.shop.analytics.repository.PageViewRepository;
import com.forme.shop.common.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final PageViewRepository pageViewRepository;

    public void record(PageViewRequest request) {
        // request.getLoginId()는 믿지 않는다 — 이 엔드포인트(/api/analytics/track)는 비로그인
        // 방문자도 호출해야 해서 SecurityConfig에서 permitAll이라, 클라이언트가 보낸 loginId를
        // 그대로 저장하면 누구나 아무 이메일이나 적어서 관리자 통계(회원별 방문 기록)를
        // 오염시킬 수 있었다. JwtFilter는 permitAll 라우트에서도 유효한 쿠키가 있으면 항상
        // SecurityContext를 채워두므로, 실제 로그인 여부는 SecurityUtil.getCurrentEmail()로
        // 서버가 직접 판단하고(비로그인이면 null — 기존과 동일하게 익명 기록) 클라이언트 값은 쓰지 않는다.
        pageViewRepository.save(new PageView(
                SecurityUtil.getCurrentEmail(), request.getPageName(),
                request.getPagePath(), request.getDuration()
        ));
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("totalPages", pageViewRepository.countDistinctPages());
        s.put("activeUsers", pageViewRepository.countDistinctUsers());
        s.put("totalViews", pageViewRepository.count());
        s.put("avgDuration", Math.round(pageViewRepository.avgDuration() * 10.0) / 10.0);
        return s;
    }

    public List<Map<String, Object>> getPageStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : pageViewRepository.findPageStats()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("pageName", row[0]);
            m.put("avgDuration", Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0);
            m.put("views", ((Number) row[2]).longValue());
            result.add(m);
        }
        return result;
    }

    public List<Map<String, Object>> getUserStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : pageViewRepository.findUserStats()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("loginId", row[0]);
            m.put("views", ((Number) row[1]).longValue());
            m.put("avgDuration", Math.round(((Number) row[2]).doubleValue() * 10.0) / 10.0);
            result.add(m);
        }
        return result;
    }

    public List<Map<String, Object>> getHourlyStats() {
        Map<Integer, Long> hourMap = new LinkedHashMap<>();
        for (int i = 0; i < 24; i++) hourMap.put(i, 0L);
        for (Object[] row : pageViewRepository.findHourlyStats()) {
            hourMap.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        hourMap.forEach((h, v) -> result.add(Map.of("hour", h, "views", v)));
        return result;
    }

    public List<PageView> getRecentViews() {
        return pageViewRepository.findTop50ByOrderByCreatedAtDesc();
    }

    // 상품 상세 페이지별 체류시간 (제품명 포함)
    public List<Map<String, Object>> getProductDetailStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : pageViewRepository.findProductDetailStats()) {
            String path = (String) row[0]; // /products/405
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("pagePath", path);
            // ID 추출
            String idStr = path.replaceAll(".*/products/", "");
            m.put("productId", idStr);
            m.put("avgDuration", Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0);
            m.put("views", ((Number) row[2]).longValue());
            result.add(m);
        }
        return result;
    }
}
