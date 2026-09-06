package com.forme.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// 토스 결제 취소(환불) API 호출처럼 느려질 수 있는 외부 호출을, 그 응답을 기다리는 사용자
// 요청 스레드/DB 커넥션과 분리해 별도 스레드에서 처리하기 위한 실행기.
// (PaymentService.refundForCancellation 참고 — 주문 취소는 커밋되면 바로 응답하고, 실제
// 토스 환불 시도는 이 풀에서 뒤이어 처리된다)
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "refundExecutor")
    public Executor refundExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("refund-");
        // 기본값(false)이면 애플리케이션 재시작/배포 시 스프링이 진행 중이거나 대기 중인 환불
        // 작업을 그냥 끊어버릴 수 있다 — 그러면 "환불 필요" 사실이 REFUND_PENDING으로 DB에는
        // 남아도 실제 시도 자체가 배포 때마다 통째로 스킵될 수 있음. 정상적인 배포(프로세스가
        // 강제 종료되는 크래시가 아니라, 종료 신호를 받고 정리할 시간이 있는 경우)라면 최소한
        // 이 시간만큼은 기다렸다가 종료하도록 한다. 작업 하나(PaymentService.refundForCancellation)
        // 가 한 주문에 연결된 결제를 순차로 여러 건 처리할 수 있어(order_id에 DB 유니크 제약이
        // 없음 — PaymentRepository 참고), 토스 호출 타임아웃(5s+10s=15s) 한 번이 아니라 그게
        // 몇 번 이어질 여유까지 감안해 넉넉히 잡는다.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(45);
        executor.initialize();
        return executor;
    }
}
