package com.roykhan.dddorderboundary.product.exception.in;

import com.roykhan.dddorderboundary.common.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 들어온 요청을 유스케이스가 받아들일 수 없는 경우.
 */
@Getter
public enum ProductErrorCode implements BaseErrorCode {
    PRODUCT_ALREADY_EXIST(HttpStatus.CONFLICT, "이미 등록된 상품입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ProductErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
