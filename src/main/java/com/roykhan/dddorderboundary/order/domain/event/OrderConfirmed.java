package com.roykhan.dddorderboundary.order.domain.event;

import com.roykhan.dddorderboundary.order.domain.model.Order;
import java.time.LocalDateTime;

// 주문 확정 - 결제 성공으로 예약이 확정되었다.
// 발생 시각은 주문이 상태를 바꾸며 남긴 시각을 그대로 쓴다
public record OrderConfirmed(long orderId, LocalDateTime occurredAt) implements OrderEvent {

    public static OrderConfirmed from(Order order) {
        return new OrderConfirmed(order.getId(), order.getConfirmedAt());
    }
}
