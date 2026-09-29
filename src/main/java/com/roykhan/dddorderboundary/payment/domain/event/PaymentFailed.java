package com.roykhan.dddorderboundary.payment.domain.event;

import java.time.LocalDateTime;

public record PaymentFailed(long orderId, LocalDateTime occurredAt) implements PaymentEvent {

    public static PaymentFailed of(long orderId) {
        return new PaymentFailed(orderId, LocalDateTime.now());
    }
}
