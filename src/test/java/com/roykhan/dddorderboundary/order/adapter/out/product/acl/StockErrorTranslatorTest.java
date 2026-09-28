package com.roykhan.dddorderboundary.order.adapter.out.product.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.roykhan.dddorderboundary.common.exception.BaseErrorCode;
import com.roykhan.dddorderboundary.common.exception.BusinessException;
import com.roykhan.dddorderboundary.order.exception.out.StockPortErrorCode;
import com.roykhan.dddorderboundary.product.exception.domain.ReservationErrorCode;
import com.roykhan.dddorderboundary.product.exception.domain.StockErrorCode;
import com.roykhan.dddorderboundary.product.exception.in.ProductErrorCode;
import com.roykhan.dddorderboundary.product.exception.out.ProductLookupErrorCode;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("StockErrorTranslator 단위 테스트 (ACL)")
class StockErrorTranslatorTest {

    private final StockErrorTranslator translator = new StockErrorTranslator();

    static Stream<Arguments> 번역표() {
        return Stream.of(
            Arguments.of(StockErrorCode.OUT_OF_STOCK, StockPortErrorCode.STOCK_NOT_ENOUGH),
            Arguments.of(StockErrorCode.INVALID_QUANTITY, StockPortErrorCode.ORDER_ITEM_NOT_ORDERABLE),
            Arguments.of(ProductLookupErrorCode.STOCK_NOT_FOUND, StockPortErrorCode.ORDER_ITEM_NOT_ORDERABLE),
            Arguments.of(ProductLookupErrorCode.PRODUCT_NOT_FOUND, StockPortErrorCode.ORDER_ITEM_NOT_ORDERABLE),
            Arguments.of(ReservationErrorCode.RESERVATION_EXPIRED, StockPortErrorCode.STOCK_RESERVATION_EXPIRED),
            Arguments.of(ReservationErrorCode.RESERVATION_NOT_CONFIRMABLE,
                StockPortErrorCode.STOCK_RESERVATION_NOT_CHANGEABLE),
            Arguments.of(ReservationErrorCode.RESERVATION_NOT_CANCELLABLE,
                StockPortErrorCode.STOCK_RESERVATION_NOT_CHANGEABLE));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("상품 컨텍스트의 코드를 주문의 코드로 바꿔 던진다")
    @MethodSource("번역표")
    void 번역(BaseErrorCode 상품_코드, StockPortErrorCode 주문_코드) {
        assertThatThrownBy(() -> translator.run(() -> {
            throw 상품_코드.exception();
        }))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(주문_코드);
    }

    // 번역표에 없는 코드까지 통과시키면 경계가 뚫린다. 모르는 것은 주문 쪽 일반 실패로 덮는다
    @Test
    @DisplayName("번역표에 없는 상품 컨텍스트 코드는 STOCK_PORT_FAILED 로 덮는다")
    void 모르는_코드() {
        assertThatThrownBy(() -> translator.run(() -> {
            throw ProductErrorCode.PRODUCT_ALREADY_EXIST.exception();
        }))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(StockPortErrorCode.STOCK_PORT_FAILED);
    }

    @Test
    @DisplayName("어떤 상품 컨텍스트 코드도 포트를 넘어오지 않는다")
    void 경계_확인() {
        Stream.of(StockErrorCode.values(), ReservationErrorCode.values(), ProductLookupErrorCode.values(),
                ProductErrorCode.values())
            .flatMap(Stream::of)
            .forEach(상품_코드 -> {
                BusinessException thrown = catchBusinessException(상품_코드);
                assertThat(thrown.getErrorCode()).isInstanceOf(StockPortErrorCode.class);
            });
    }

    @Test
    @DisplayName("예외가 없으면 호출을 그대로 통과시킨다")
    void 정상_통과() {
        assertThatCode(() -> translator.run(() -> {
        })).doesNotThrowAnyException();
    }

    private BusinessException catchBusinessException(BaseErrorCode errorCode) {
        try {
            translator.run(() -> {
                throw errorCode.exception();
            });
            throw new AssertionError("예외가 던져지지 않았다: " + errorCode.name());
        } catch (BusinessException e) {
            return e;
        }
    }
}
