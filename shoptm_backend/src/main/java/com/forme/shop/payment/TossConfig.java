package com.forme.shop.payment;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class TossConfig {

    @Value("${toss.client-key:test_ck_placeholder}")
    private String clientKey;

    @Value("${toss.secret-key:test_sk_placeholder}")
    private String secretKey;

    public static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
}
