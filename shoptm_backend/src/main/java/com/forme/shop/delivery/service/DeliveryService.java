package com.forme.shop.delivery.service;

import com.forme.shop.common.security.SecurityUtil;
import com.forme.shop.delivery.dto.DeliveryRequestDto;
import com.forme.shop.delivery.dto.DeliveryResponseDto;
import com.forme.shop.delivery.entity.Delivery;
import com.forme.shop.delivery.repository.DeliveryRepository;
import com.forme.shop.order.entity.Orders;
import com.forme.shop.order.repository.OrderRepository;
import com.forme.shop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    // Delivery 엔티티 Javadoc에 적힌 흐름과 동일한 순서 — DeliveryRequestDto.Update.status의
    // @Pattern 화이트리스트와도 맞춰, 여기 없는 값은 애초에 요청 검증에서 걸러진다.
    private static final List<String> STATUS_FLOW = List.of("READY", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED");
    private static final int IN_TRANSIT_IDX = STATUS_FLOW.indexOf("IN_TRANSIT");
    private static final int DELIVERED_IDX = STATUS_FLOW.indexOf("DELIVERED");

    // 특정 주문의 배송 정보 조회 — 존재 여부 확인과 소유자 확인을 한 번에 처리하는 이유는
    // OrderService.findSelfOrAdminOrder() 주석 참고 (주문 id 열거 방지)
    public DeliveryResponseDto getDelivery(Long orderId) {
        orderService.findSelfOrAdminOrder(orderId);

        Delivery delivery = deliveryRepository.findByOrdersId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("배송 정보가 없습니다."));
        return DeliveryResponseDto.from(delivery);
    }

    // 배송 상태별 목록 조회 (관리자)
    // ex: status = "IN_TRANSIT" → 배송중인 것만 조회
    public List<DeliveryResponseDto> getDeliveriesByStatus(String status) {
        SecurityUtil.requireAdmin();
        return deliveryRepository.findByStatus(status)
                .stream()
                .map(DeliveryResponseDto::from)
                .collect(Collectors.toList());
    }

    // 배송 정보 등록 (관리자)
    // 주문 생성 후 배송 정보를 별도로 등록할 때 사용
    @Transactional
    public DeliveryResponseDto createDelivery(Long orderId, DeliveryRequestDto.Create dto) {
        SecurityUtil.requireAdmin();
        // 주문 존재 여부 확인
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 이미 배송 정보가 있는지 확인
        if (deliveryRepository.findByOrdersId(orderId).isPresent()) {
            throw new IllegalArgumentException("이미 배송 정보가 등록되어 있습니다.");
        }

        Delivery delivery = Delivery.builder()
                .orders(orders)
                .carrier(dto.getCarrier())
                .trackingNumber(dto.getTrackingNumber())
                .build();

        return DeliveryResponseDto.from(deliveryRepository.save(delivery));
    }

    // 배송 정보 수정 (관리자)
    // 운송장 번호 입력, 배송 상태 변경 등
    @Transactional
    public DeliveryResponseDto updateDelivery(Long deliveryId, DeliveryRequestDto.Update dto) {
        SecurityUtil.requireAdmin();
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배송 정보입니다."));

        // 관리자가 이 배송 건의 화면을 열어둔 사이(이중클릭, 또는 다른 관리자의 동시 처리)에
        // 상태가 이미 바뀌었으면 그 변경을 덮어쓰지 않고 거부한다 — ProductService.updateProduct()
        // 와 동일한 이유(교차검증에서 발견). expectedUpdatedAt이 없으면(구버전 클라이언트 등)
        // 이 확인을 건너뛴다.
        if (dto.getExpectedUpdatedAt() != null && !dto.getExpectedUpdatedAt().equals(delivery.getUpdatedAt())) {
            throw new IllegalArgumentException(
                    "다른 곳에서 이미 이 배송 정보가 변경되었습니다. 새로고침 후 다시 시도해주세요.");
        }

        // null 체크 후 값이 있을 때만 수정
        if (dto.getCarrier()        != null) delivery.setCarrier(dto.getCarrier());
        if (dto.getTrackingNumber() != null) delivery.setTrackingNumber(dto.getTrackingNumber());

        // 배송 상태 변경 시 시간 자동 기록 — 상태값 자체가 아니라 흐름상 순서를 기준으로
        // shippedAt/deliveredAt을 맞춘다. 예전엔 "IN_TRANSIT으로 바뀔 때만" 발송 시간을
        // 기록해서, OUT_FOR_DELIVERY로 바로 건너뛰면 발송 시간이 영영 안 남고, 반대로
        // DELIVERED로 잘못 찍었다가 IN_TRANSIT으로 되돌려도 deliveredAt이 안 지워져
        // "상태는 배송중인데 배송완료 시각은 과거에 찍혀있는" 모순이 남았었다.
        if (dto.getStatus() != null) {
            delivery.setStatus(dto.getStatus());
            int idx = STATUS_FLOW.indexOf(dto.getStatus());

            // IN_TRANSIT 이상(배달중·배송완료 포함)이면 발송 시간이 있어야 하고,
            // READY로 되돌아갔다면 아직 발송 전이므로 지운다.
            if (idx >= IN_TRANSIT_IDX) {
                if (delivery.getShippedAt() == null) delivery.setShippedAt(LocalDateTime.now());
            } else {
                delivery.setShippedAt(null);
            }

            // DELIVERED일 때만 배송완료 시간이 있어야 하고, 그 외 상태로 바뀌면(되돌림 포함) 지운다.
            if (idx == DELIVERED_IDX) {
                if (delivery.getDeliveredAt() == null) delivery.setDeliveredAt(LocalDateTime.now());
            } else {
                delivery.setDeliveredAt(null);
            }
        }

        // saveAndFlush로 이 자리에서 즉시 UPDATE를 실행한다. 두 가지 이유: (1) 위의
        // expectedUpdatedAt 확인은 "읽은 뒤 쓰기 전" 사이의 시간차에만 걸리는 애플리케이션
        // 레벨 사전 확인이라, 두 요청이 거의 동시에 들어와 둘 다 그 확인을 통과한 뒤에야 쓰는
        // 진짜 동시 레이스는 못 막는데, @Version이 바로 이 UPDATE에 "WHERE version = 읽은 값"을
        // 자동으로 붙여 그 틈까지 DB 차원에서 막아준다(실패하면 OptimisticLockingFailureException,
        // GlobalExceptionHandler에서 같은 안내 메시지로 처리). (2) saveAndFlush 없이 더티체킹에만
        // 맡기면 커밋이 이 메서드 반환 이후에야 일어나서, 바로 아래 응답에 담기는 updatedAt이
        // 이번 수정이 반영되기 전의 값이 된다 — 그러면 바로 다음 수정 요청이 그 (이미 틀린)
        // updatedAt을 expectedUpdatedAt으로 그대로 돌려보내 아무도 안 건드렸는데도 "다른 곳에서
        // 이미 변경됨" 오류가 나는 모순이 생긴다(교차검증에서 발견).
        deliveryRepository.saveAndFlush(delivery);
        return DeliveryResponseDto.from(delivery);
    }
}