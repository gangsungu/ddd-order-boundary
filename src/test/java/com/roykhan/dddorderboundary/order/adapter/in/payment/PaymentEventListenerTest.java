package com.roykhan.dddorderboundary.order.adapter.in.payment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.roykhan.dddorderboundary.order.application.port.in.OrderUseCase;
import com.roykhan.dddorderboundary.order.exception.domain.OrderErrorCode;
import com.roykhan.dddorderboundary.payment.domain.event.PaymentFailed;
import com.roykhan.dddorderboundary.payment.domain.event.PaymentSucceeded;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventListener 단위 테스트")
class PaymentEventListenerTest {

    @Mock
    private OrderUseCase orderUseCase;

    @InjectMocks
    private PaymentEventListener listener;

    @Test
    @DisplayName("PaymentSucceeded 를 주문 확정으로 번역한다")
    void 결제_성공_이벤트() {
        listener.on(PaymentSucceeded.of(7L));

        verify(orderUseCase).confirmOrder(7L);
        verify(orderUseCase, never()).failPayment(anyLong());
    }

    @Test
    @DisplayName("PaymentFailed 를 주문의 결제 실패 반영(예약 해제)으로 번역한다")
    void 결제_실패_이벤트() {
        listener.on(PaymentFailed.of(7L));

        verify(orderUseCase).failPayment(7L);
        verify(orderUseCase, never()).confirmOrder(anyLong());
    }

    @Test
    @DisplayName("주문이 결과를 반영하지 못해도 주문의 에러 코드는 결제 쪽으로 던지지 않는다")
    void 반영_거절() {
        doThrow(OrderErrorCode.ORDER_ALREADY_EXPIRED.exception()).when(orderUseCase).confirmOrder(7L);

        assertThatCode(() -> listener.on(PaymentSucceeded.of(7L))).doesNotThrowAnyException();
    }
}
