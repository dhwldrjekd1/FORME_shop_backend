package com.forme.shop.analytics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "page_views")
public class PageView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50)
    private String loginId;

    @Column(length = 100, nullable = false)
    private String pageName;

    @Column(length = 255, nullable = false)
    private String pagePath;

    @Column(nullable = false)
    private Integer duration;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public PageView() {}

    public PageView(String loginId, String pageName, String pagePath, Integer duration) {
        this.loginId = loginId;
        this.pageName = pageName;
        this.pagePath = pagePath;
        this.duration = duration;
        this.createdAt = LocalDateTime.now();
    }
}
