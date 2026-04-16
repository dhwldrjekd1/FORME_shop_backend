package com.forme.shop.analytics.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageViewRequest {
    private String loginId;
    private String pageName;
    private String pagePath;
    private Integer duration;
}
