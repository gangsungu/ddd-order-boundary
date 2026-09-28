package com.roykhan.dddorderboundary.order.exception.out;

import com.roykhan.dddorderboundary.common.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 재고 포트가 실패했을 때 주문이 쓰는 코드.
 *
 * <p>상품 컨텍스트의 코드는 번역 계층(ACL)에서 이 코드로 바뀐다.
 * 주문은 상품 컨텍스트의 코드를 모르고, 그 코드가 주문 API 응답까지 새지도 않는다.
 */
@Getter
public enum StockPortErrorCode implements BaseErrorCode {
    STOCK_NOT_ENOUGH(HttpStatus.CONFLICT, "주문 수량만큼 재고가 남아 있지 않습니다."),
    ORDER_ITEM_NOT_ORDERABLE(HttpStatus.BAD_REQUEST, "주문할 수 없는 항목이 포함되어 있습니다."),
    STOCK_RESERVATION_EXPIRED(HttpStatus.CONFLICT, "재고 예약이 만료되어 처리할 수 없습니다."),
    STOCK_RESERVATION_NOT_CHANGEABLE(HttpStatus.CONFLICT, "재고 예약을 변경할 수 없는 상태입니다."),
    STOCK_PORT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "재고 처리를 완료하지 못했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    StockPortErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
