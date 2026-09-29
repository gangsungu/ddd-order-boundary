package com.roykhan.dddorderboundary.payment.domain.event;

import java.time.LocalDateTime;

public record PaymentSucceeded(long orderId, LocalDateTime occurredAt) implements PaymentEvent {

    public static PaymentSucceeded of(long orderId) {
        return new PaymentSucceeded(orderId, LocalDateTime.now());
    }
}
