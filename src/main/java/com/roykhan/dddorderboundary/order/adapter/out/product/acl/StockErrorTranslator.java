package com.roykhan.dddorderboundary.order.adapter.out.product.acl;

import com.roykhan.dddorderboundary.common.exception.BaseErrorCode;
import com.roykhan.dddorderboundary.common.exception.BusinessException;
import com.roykhan.dddorderboundary.order.exception.out.StockPortErrorCode;
import com.roykhan.dddorderboundary.product.exception.domain.ReservationErrorCode;
import com.roykhan.dddorderboundary.product.exception.domain.StockErrorCode;
import com.roykhan.dddorderboundary.product.exception.out.ProductLookupErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 번역 계층(ACL) — 상품 컨텍스트가 던진 예외를 주문의 언어로 옮긴다.
 *
 * <p>출력 어댑터는 상대 컨텍스트를 알아도 되지만, 상대의 에러 코드가 포트를 넘어와서는 안 된다.
 * 그 경계를 지키는 곳이 여기 하나뿐이라, 섹션 4 에서 호출이 네트워크를 건너가도 번역할 자리는 그대로 남는다.
 */
@Slf4j
@Component
public class StockErrorTranslator {

    /**
     * 재고 유스케이스 호출을 감싼다. 상품 컨텍스트의 예외는 주문 코드로 바뀌어 다시 던져진다.
     */
    public void run(Runnable call) {
        try {
            call.run();
        } catch (BusinessException e) {
            throw toOrderException(e);
        }
    }

    private BusinessException toOrderException(BusinessException e) {
        StockPortErrorCode translated = translate(e.getErrorCode());

        // 번역표에 없는 코드까지 그대로 흘리면 경계가 뚫린다. 원본은 로그로만 남긴다
        if (translated == null) {
            log.warn("번역하지 못한 상품 컨텍스트 예외: code={}, message={}", e.getErrorCode().name(), e.getMessage());
            return StockPortErrorCode.STOCK_PORT_FAILED.exception();
        }

        return translated.exception();
    }

    private StockPortErrorCode translate(BaseErrorCode foreign) {
        if (foreign instanceof StockErrorCode code) {
            return switch (code) {
                case OUT_OF_STOCK -> StockPortErrorCode.STOCK_NOT_ENOUGH;
                case INVALID_QUANTITY -> StockPortErrorCode.ORDER_ITEM_NOT_ORDERABLE;
            };
        }

        if (foreign instanceof ReservationErrorCode code) {
            return switch (code) {
                case RESERVATION_EXPIRED -> StockPortErrorCode.STOCK_RESERVATION_EXPIRED;
                case RESERVATION_NOT_CONFIRMABLE, RESERVATION_NOT_CANCELLABLE ->
                    StockPortErrorCode.STOCK_RESERVATION_NOT_CHANGEABLE;
            };
        }

        if (foreign instanceof ProductLookupErrorCode code) {
            return switch (code) {
                case STOCK_NOT_FOUND, PRODUCT_NOT_FOUND -> StockPortErrorCode.ORDER_ITEM_NOT_ORDERABLE;
            };
        }

        return null;
    }
}
