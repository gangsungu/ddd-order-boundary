package com.roykhan.dddorderboundary.order.exception.out;

import com.roykhan.dddorderboundary.common.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 출력 포트를 부른 결과 "없다"는 답이 온 경우.
 *
 * <p>주문 저장소(`OrderRepository`)와 상품 컨텍스트(`ProductPort`) 모두 여기에 해당한다.
 */
@Getter
public enum OrderLookupErrorCode implements BaseErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    INVALID_ORDER_ITEM(HttpStatus.BAD_REQUEST, "존재하지 않는 상품이 포함되어 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    OrderLookupErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
