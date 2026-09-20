package com.roykhan.dddorderboundary.domain.stock.enums;

import lombok.Getter;

@Getter
public enum ReservationStatus {
    RESERVED("예약"),
    CONFIRMED("예약 확정"),
    CANCELLED("예약 해제"),
    EXPIRED("예약 만료");

    private final String description;

    ReservationStatus(final String description) {
        this.description = description;
    }
}
