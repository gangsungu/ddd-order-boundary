package com.roykhan.dddorderboundary.payment.infrastructure.order;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.roykhan.dddorderboundary.order.application.usecase.OrderUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderAdapter 단위 테스트")
class OrderAdapterTest {

    @Mock
    private OrderUseCase orderUseCase;

    @InjectMocks
    private OrderAdapter orderAdapter;

    @Test
    @DisplayName("결제 성공 알림을 주문 확정으로 옮긴다")
    void 결제_성공_알림() {
        orderAdapter.notifyPaymentSucceeded(7L);

        verify(orderUseCase).confirmOrder(7L);
        verify(orderUseCase, never()).failPayment(anyLong());
    }

    @Test
    @DisplayName("결제 실패 알림을 주문의 결제 실패 반영(예약 해제)으로 옮긴다")
    void 결제_실패_알림() {
        orderAdapter.notifyPaymentFailed(7L);

        verify(orderUseCase).failPayment(7L);
        verify(orderUseCase, never()).confirmOrder(anyLong());
    }
}
