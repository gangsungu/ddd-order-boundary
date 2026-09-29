package com.roykhan.dddorderboundary.order.domain.event;

import com.roykhan.dddorderboundary.order.domain.model.Order;
import java.time.LocalDateTime;

// 주문 만료 - 결제 마감까지 결제되지 않아 예약이 만료되었다.
// 발생 시각은 주문이 상태를 바꾸며 남긴 시각을 그대로 쓴다
public record OrderExpired(long orderId, LocalDateTime occurredAt) implements OrderEvent {

    public static OrderExpired from(Order order) {
        return new OrderExpired(order.getId(), order.getCancelledAt());
    }
}
