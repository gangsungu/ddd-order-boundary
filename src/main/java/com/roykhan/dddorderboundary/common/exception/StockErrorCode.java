package com.roykhan.dddorderboundary.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum StockErrorCode implements BaseErrorCode {
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "재고를 찾을 수 없습니다."),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "수량은 1개 이상이어야 합니다.");

    private final HttpStatus httpStatus;
    private final String message;

    StockErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
