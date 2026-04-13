package com.forme.shop.analytics.service;

import com.forme.shop.analytics.dto.PageViewRequest;
import com.forme.shop.analytics.entity.PageView;
import com.forme.shop.analytics.repository.PageViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final PageViewRepository pageViewRepository;

    public void record(PageViewRequest request) {
        pageViewRepository.save(new PageView(
                request.getLoginId(), request.getPageName(),
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
}
