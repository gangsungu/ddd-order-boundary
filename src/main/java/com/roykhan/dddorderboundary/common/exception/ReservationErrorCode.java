package com.roykhan.dddorderboundary.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ReservationErrorCode implements BaseErrorCode {
    RESERVATION_NOT_CONFIRMABLE(HttpStatus.CONFLICT, "확정할 수 없는 예약 상태입니다."),
    RESERVATION_NOT_CANCELLABLE(HttpStatus.CONFLICT, "취소할 수 없는 예약 상태입니다."),
    RESERVATION_EXPIRED(HttpStatus.CONFLICT, "만료된 예약입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ReservationErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
