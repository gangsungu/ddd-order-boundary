package com.roykhan.dddorderboundary.payment.domain.model;

import lombok.Getter;

@Getter
public enum PaymentResult {
    SUCCESS("결제 성공"),
    FAILURE("결제 실패");

    private final String description;

    PaymentResult(final String description) {
        this.description = description;
    }
}
