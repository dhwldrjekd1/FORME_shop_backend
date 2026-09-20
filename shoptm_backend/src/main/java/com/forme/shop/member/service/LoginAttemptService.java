package com.forme.shop.member.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

// 로그인 무제한 시도(브루트포스/크리덴셜 스터핑) 방지용 잠금 카운터.
// key는 로그인 시도에 사용된 이메일 문자열 그대로(계정 존재 여부와 무관하게 기록) —
// 존재하는 계정만 카운트하면, 6번째 시도부터 메시지가 "이메일 또는 비밀번호가 틀렸습니다"에서
// "너무 많은 시도"로 바뀌는 차이만으로 그 이메일이 실제 가입된 계정인지 알아낼 수 있는
// 새로운 열거(enumeration) 통로가 생기기 때문에, 실패 원인을 가리지 않고 동일하게 기록한다.
// 이메일 단위로만 잠그기 때문에, 남의 이메일을 알고 있는 공격자가 일부러 틀린 비밀번호를
// 반복 제출해 그 계정의 정상 로그인을 15분간 막는 것도 이론상 가능하다(서비스 거부) —
// 다만 이건 이메일 기반 잠금 방식 자체의 잘 알려진 트레이드오프이고, IP 기반 제한까지
// 함께 두려면 추적 대상과 정리 로직이 늘어나 이 프로젝트 규모에 비해 과할 수 있어
// 우선 이메일 단위로만 두고, 필요해지면 그때 확장한다.
//
// 저장소는 접근 순서 기준 LRU: 크기가 상한(MAX_TRACKED_EMAILS)을 넘으면 가장 오래전에
// 손댄 항목부터 자동으로 밀어낸다. "상한에 도달하면 새 이메일은 아예 추적하지 않는다"는
// 방식은, 공격자가 먼저 더미 이메일 수만 개로 맵을 가득 채운 뒤 한 번도 시도되지 않았던
// 진짜 타깃 계정을 무제한으로 대입하는 우회를 그대로 열어주기 때문에 쓰지 않는다 — LRU면
// 그 타깃 이메일도 항상 새로 추적되고(가장 최근 항목이라 밀려나지 않음), 대신 오래전에
// 실패했던 무관한 이메일들의 기록이 먼저 밀려날 뿐이라 안전하다.
// 전체를 하나의 락으로 감싸는 대신 ConcurrentHashMap + 별도 정리 스레드 조합도 가능하지만,
// 이 프로젝트 규모(소형 쇼핑몰)의 로그인 트래픽에서 synchronized 정도의 경합은 병목이 되지
// 않고, LRU 제거/증가/리셋을 한 락 안에서 처리하는 편이 레이스를 걱정할 필요가 없어 더 안전함.
// 단일 서버 인스턴스 기준 메모리 구현 — TokenBlacklistService와 동일한 이유로,
// 서버를 여러 대로 늘리면 Redis 같은 공유 저장소로 바꿔야 한다. 서버 재시작 시 카운터가
// 풀리는 것은 로그인 잠금 특성상 보안 저하가 크지 않아(재시작은 배포 시에만 발생) 허용한다.
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCKOUT = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_EMAILS = 50_000;

    private static class Attempts {
        int count = 0;
        Instant lastFailureAt = Instant.now();
        Instant lockedUntil = null;
    }

    // accessOrder=true LinkedHashMap → get/put이 그 항목을 "가장 최근" 자리로 옮김.
    // 자동 removeEldestEntry는 쓰지 않는다 — 그걸 쓰면 "가장 오래 손 안 댄" 항목을 무조건
    // 지우는데, 막 5회 실패해서 잠긴 계정은 그 이후로는 로그인 시도 자체가 막혀 손이 안 가니
    // 오히려 가장 먼저 밀려나는 항목이 돼버린다 — 공격자가 무관한 더미 이메일 수만 개로
    // 맵을 채우면 이미 걸어둔 잠금이 그걸로 지워져 풀려버리는 것(evictIfNeeded 참고).
    private final Map<String, Attempts> attemptsByEmail =
            Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true));

    // 잠겨있으면 true — 로그인 시도 자체(비밀번호 확인 전)를 여기서 막는다.
    public boolean isLocked(String email) {
        synchronized (attemptsByEmail) {
            Attempts a = attemptsByEmail.get(normalize(email));
            return a != null && a.lockedUntil != null && Instant.now().isBefore(a.lockedUntil);
        }
    }

    // 로그인 실패(계정 없음/비밀번호 틀림/비활성 계정 등, 사유 불문) 시 호출.
    public void recordFailure(String email) {
        String key = normalize(email);
        synchronized (attemptsByEmail) {
            Attempts a = attemptsByEmail.get(key);
            if (a == null) {
                evictIfAtCapacity();
                a = new Attempts();
                attemptsByEmail.put(key, a);
            }
            Instant now = Instant.now();

            // 고정된 창(윈도우) 시작 시각 기준으로 리셋하면, 창 경계를 걸쳐 절반씩 나눠 시도해
            // 실제로는 MAX_ATTEMPTS의 거의 두 배까지 잠금 없이 시도할 수 있다. 대신 "마지막 실패
            // 이후 WINDOW만큼 조용했는지"를 기준으로 리셋해, 공격자가 쉬지 않고 계속 시도하는 한
            // 카운트가 끊기지 않도록 한다.
            if (now.isAfter(a.lastFailureAt.plus(WINDOW))) {
                a.count = 0;
            }
            a.lastFailureAt = now;

            if (++a.count >= MAX_ATTEMPTS) {
                a.lockedUntil = now.plus(LOCKOUT);
            }
        }
    }

    // 로그인 성공 시 호출 — 그 이메일의 실패 기록을 지운다.
    public void recordSuccess(String email) {
        attemptsByEmail.remove(normalize(email));
    }

    // 상한에 도달했을 때 한 자리를 비운다. 지금 실제로 잠겨있는 항목은 건너뛰고,
    // 잠겨있지 않은 것 중 가장 오래 손 안 댄 항목부터 지운다 — 그래야 공격자가 무관한
    // 더미 이메일을 아무리 많이 채워도, 이미 걸어둔 진짜 계정의 잠금은 끝까지 유지된다.
    // (모든 항목이 다 잠겨있는 극단적인 경우에만 어쩔 수 없이 가장 오래된 잠금부터 지운다 —
    // 이 프로젝트 규모에서 5만 개 계정이 동시에 잠기는 건 현실적으로 일어나지 않는다.)
    private void evictIfAtCapacity() {
        if (attemptsByEmail.size() < MAX_TRACKED_EMAILS) return;
        Instant now = Instant.now();
        Iterator<Map.Entry<String, Attempts>> it = attemptsByEmail.entrySet().iterator();
        while (it.hasNext()) {
            Attempts a = it.next().getValue();
            boolean activelyLocked = a.lockedUntil != null && now.isBefore(a.lockedUntil);
            if (!activelyLocked) {
                it.remove();
                return;
            }
        }
        it = attemptsByEmail.entrySet().iterator();
        if (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
