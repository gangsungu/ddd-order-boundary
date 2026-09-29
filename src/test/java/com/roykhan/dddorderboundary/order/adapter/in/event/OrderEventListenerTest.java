package com.roykhan.dddorderboundary.order.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.roykhan.dddorderboundary.order.application.port.in.OrderProjectionUseCase;
import com.roykhan.dddorderboundary.order.domain.event.OrderConfirmed;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventListener 단위 테스트")
class OrderEventListenerTest {

    @Mock
    private OrderProjectionUseCase orderProjectionUseCase;

    @InjectMocks
    private OrderEventListener listener;

    @Test
    @DisplayName("받은 주문 이벤트를 읽기 모델 갱신으로 넘긴다")
    void 이벤트_전달() {
        OrderConfirmed event = new OrderConfirmed(1L, LocalDateTime.now());

        listener.on(event);

        verify(orderProjectionUseCase).project(event);
    }

    @Test
    @DisplayName("읽기 모델 갱신이 실패해도 이미 커밋된 쓰기 쪽으로 예외를 던지지 않는다")
    void 갱신_실패() {
        OrderConfirmed event = new OrderConfirmed(1L, LocalDateTime.now());
        doThrow(new IllegalStateException("db down")).when(orderProjectionUseCase).project(event);

        assertThatCode(() -> listener.on(event)).doesNotThrowAnyException();
    }
}
