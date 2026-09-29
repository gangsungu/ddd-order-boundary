package com.roykhan.dddorderboundary.order.adapter.in.payment;

import com.roykhan.dddorderboundary.common.exception.BusinessException;
import com.roykhan.dddorderboundary.order.application.port.in.OrderUseCase;
import com.roykhan.dddorderboundary.payment.domain.event.PaymentEvent;
import com.roykhan.dddorderboundary.payment.domain.event.PaymentFailed;
import com.roykhan.dddorderboundary.payment.domain.event.PaymentSucceeded;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 입력 어댑터 — 결제 이벤트를 주문의 언어로 번역해 받는다 (ACL).
 *
 * <p>결제의 이벤트 계약을 아는 곳은 주문 안에서 여기 하나뿐이다. 결제의 "성공했다/실패했다"를
 * 주문의 "확정한다/결제 실패로 돌린다"로 옮기고, 그 뒤의 일은 유스케이스가 한다.
 *
 * <p>번역은 반대 방향도 막는다. 주문이 결과를 반영하지 못해도 주문의 에러 코드는 결제로 돌아가지 않고
 * 여기서 로그로 끝난다. 결제 성공인데 주문이 받지 못한 경우의 보상(환불)은 Saga 에서 다룬다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderUseCase orderUseCase;

    // 결제 트랜잭션이 커밋된 뒤에만 받는다. 롤백된 결제는 주문에 닿지 않는다.
    // 커밋 직후에는 끝난 결제 트랜잭션이 아직 묶여 있어, 그대로 합류하면 주문 변경이 커밋되지 않는다.
    // NOT_SUPPORTED 로 떼어 두고 유스케이스가 자기 트랜잭션을 새로 연다
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void on(PaymentEvent event) {
        try {
            switch (event) {
                case PaymentSucceeded e -> orderUseCase.confirmOrder(e.orderId());
                case PaymentFailed e -> orderUseCase.failPayment(e.orderId());
            }
            log.info("결제 결과 반영: orderId={}, event={}", event.orderId(), event.getClass().getSimpleName());
        } catch (BusinessException e) {
            // 결제 결과가 오기 전에 취소·만료된 주문 등. 결제는 이미 끝났으므로 되돌려 알리지 않는다
            log.warn("결제 결과 반영 거절: orderId={}, event={}, code={}",
                event.orderId(), event.getClass().getSimpleName(), e.getErrorCode().name());
        }
    }
}
