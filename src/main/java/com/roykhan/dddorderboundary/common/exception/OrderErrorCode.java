package com.roykhan.dddorderboundary.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum OrderErrorCode implements BaseErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 주문입니다."),
    ORDER_ALREADY_EXPIRED(HttpStatus.CONFLICT, "이미 만료된 주문입니다."),
    ORDER_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확정된 주문입니다."),
    ORDER_PAYMENT_FAILED(HttpStatus.CONFLICT, "결제에 실패한 주문입니다."),
    INVALID_ORDER_ITEM(HttpStatus.BAD_REQUEST, "존재하지 않는 상품이 포함되어 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    OrderErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
