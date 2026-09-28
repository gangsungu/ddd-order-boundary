package com.roykhan.dddorderboundary.product.exception.out;

import com.roykhan.dddorderboundary.common.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 출력 포트를 부른 결과 "없다"는 답이 온 경우.
 */
@Getter
public enum ProductLookupErrorCode implements BaseErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "재고를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ProductLookupErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
