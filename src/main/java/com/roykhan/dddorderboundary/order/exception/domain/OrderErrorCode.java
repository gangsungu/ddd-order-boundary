package com.roykhan.dddorderboundary.order.exception.domain;

import com.roykhan.dddorderboundary.common.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 주문 애그리거트가 자기 상태만 보고 거절하는 경우.
 */
@Getter
public enum OrderErrorCode implements BaseErrorCode {
    ORDER_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 주문입니다."),
    ORDER_ALREADY_EXPIRED(HttpStatus.CONFLICT, "이미 만료된 주문입니다."),
    ORDER_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확정된 주문입니다."),
    ORDER_PAYMENT_FAILED(HttpStatus.CONFLICT, "결제에 실패한 주문입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    OrderErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
