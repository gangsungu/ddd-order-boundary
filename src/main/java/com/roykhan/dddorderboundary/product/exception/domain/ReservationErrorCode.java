package com.roykhan.dddorderboundary.product.exception.domain;

import com.roykhan.dddorderboundary.common.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 예약 애그리거트가 자기 상태만 보고 거절하는 경우.
 */
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
