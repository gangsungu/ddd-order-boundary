package com.roykhan.dddorderboundary.domain.payment.service;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.roykhan.dddorderboundary.domain.order.service.OrderService;
import com.roykhan.dddorderboundary.domain.payment.enums.PaymentResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("결제 성공이면 주문 확정을 요청한다")
    void 결제_성공() {
        paymentService.applyResult(1L, PaymentResult.SUCCESS);

        verify(orderService).confirmOrder(1L);
        verify(orderService, never()).failPayment(anyLong());
    }

    @Test
    @DisplayName("결제 실패면 재고 복원을 위해 결제 실패 반영을 요청한다")
    void 결제_실패() {
        paymentService.applyResult(1L, PaymentResult.FAILURE);

        verify(orderService).failPayment(1L);
        verify(orderService, never()).confirmOrder(anyLong());
    }
}
