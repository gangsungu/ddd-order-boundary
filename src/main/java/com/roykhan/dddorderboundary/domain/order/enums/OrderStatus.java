package com.roykhan.dddorderboundary.domain.order.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING("결제 대기"),
    CONFIRMED("주문 확정"),
    PAYMENT_FAILED("결제 실패"),
    CANCELLED("주문 취소"),
    EXPIRED("주문 만료");

    private final String description;

    OrderStatus(final String description) {
        this.description = description;
    }
}
