package com.roykhan.dddorderboundary.product.exception.domain;

import com.roykhan.dddorderboundary.common.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 재고 애그리거트가 자기 수량만 보고 거절하는 경우.
 */
@Getter
public enum StockErrorCode implements BaseErrorCode {
    OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "수량은 1개 이상이어야 합니다.");

    private final HttpStatus httpStatus;
    private final String message;

    StockErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
