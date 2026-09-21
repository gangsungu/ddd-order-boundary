package com.roykhan.dddorderboundary.payment.infrastructure.order;

import com.roykhan.dddorderboundary.order.application.usecase.OrderUseCase;
import com.roykhan.dddorderboundary.payment.application.port.OrderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 출력 어댑터 - 결제의 알림을 주문 컨텍스트의 유스케이스 호출로 옮긴다.
// 결제 성공은 주문 확정, 결제 실패는 예약 해제로 이어진다.
// 섹션 3 에서 이 알림을 이벤트 발행으로 바꾸면 이 어댑터만 교체한다
@Component
@RequiredArgsConstructor
public class OrderAdapter implements OrderPort {

    private final OrderUseCase orderUseCase;

    @Override
    public void notifyPaymentSucceeded(long orderId) {
        orderUseCase.confirmOrder(orderId);
    }

    @Override
    public void notifyPaymentFailed(long orderId) {
        orderUseCase.failPayment(orderId);
    }
}
