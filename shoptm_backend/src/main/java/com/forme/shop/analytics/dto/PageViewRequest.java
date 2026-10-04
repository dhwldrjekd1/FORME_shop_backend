package com.forme.shop.analytics.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageViewRequest {
    // 더 이상 쓰지 않음(AnalyticsService.record 참고 — 클라이언트가 보낸 값은 위조 가능해
    // 서버가 SecurityUtil.getCurrentEmail()로 직접 판단한 값을 대신 씀) — 그래도 필드 자체는
    // 지우면 안 됨. Jackson의 FAIL_ON_UNKNOWN_PROPERTIES가 기본값(켜짐)이라, 프론트
    // (utils/pageTracker.js)가 여전히 이 키를 보내는데 필드를 지우면 모르는 JSON 필드로
    // 걸려 트래킹 요청 자체가 전부 400으로 실패한다(ProductRequestDto.category와 동일한 이유).
    private String loginId;
    private String pageName;
    private String pagePath;
    private Integer duration;
}
