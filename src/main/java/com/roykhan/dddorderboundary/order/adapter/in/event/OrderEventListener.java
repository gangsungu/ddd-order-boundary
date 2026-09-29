package com.roykhan.dddorderboundary.order.adapter.in.event;

import com.roykhan.dddorderboundary.order.application.port.in.OrderProjectionUseCase;
import com.roykhan.dddorderboundary.order.domain.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 입력 어댑터 — 주문 이벤트를 받아 읽기 모델을 갱신한다 (CQRS).
 *
 * <p>쓰기 트랜잭션이 커밋된 뒤에만 받으므로, 롤백된 주문은 읽기 모델에 나타나지 않는다.
 * 대신 쓰기와 읽기 사이에 짧은 틈이 생긴다 (결과적 일관성).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderProjectionUseCase orderProjectionUseCase;

    // 커밋 직후에는 끝난 쓰기 트랜잭션이 아직 묶여 있어, 그대로 합류하면 읽기 모델 변경이 커밋되지 않는다.
    // NOT_SUPPORTED 로 떼어 두고 갱신이 자기 트랜잭션을 새로 연다 (PaymentEventListener 와 같은 이유)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void on(OrderEvent event) {
        try {
            orderProjectionUseCase.project(event);
        } catch (RuntimeException e) {
            // 쓰기는 이미 커밋돼 되돌릴 수 없다. 읽기 모델만 뒤처진 채로 남는다
            log.error("읽기 모델 갱신 실패: orderId={}, event={}", event.orderId(), event.getClass().getSimpleName(), e);
        }
    }
}
