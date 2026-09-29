package com.roykhan.dddorderboundary.order.domain.event;

import com.roykhan.dddorderboundary.order.domain.model.Order;
import java.time.LocalDateTime;

// 결제 실패 - 예약이 해제되었다.
// 발생 시각은 주문이 상태를 바꾸며 남긴 시각을 그대로 쓴다
public record OrderPaymentFailed(long orderId, LocalDateTime occurredAt) implements OrderEvent {

    public static OrderPaymentFailed from(Order order) {
        return new OrderPaymentFailed(order.getId(), order.getCancelledAt());
    }
}
