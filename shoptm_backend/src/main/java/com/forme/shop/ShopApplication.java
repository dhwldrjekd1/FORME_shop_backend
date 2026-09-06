package com.forme.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// EnableScheduling: 결제 없이 방치된 데모 주문을 주기적으로 자동 취소하는 데 사용
// (com.forme.shop.order.scheduler.OrderExpiryScheduler 참고)
@SpringBootApplication
@EnableScheduling
public class ShopApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopApplication.class, args);
	}

}
