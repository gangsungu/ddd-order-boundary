package com.roykhan.dddorderboundary.payment.application.service;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.roykhan.dddorderboundary.order.application.usecase.OrderUseCase;
import com.roykhan.dddorderboundary.payment.domain.model.PaymentResult;
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
    private OrderUseCase orderUseCase;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("결제 성공이면 주문 확정을 요청한다")
    void 결제_성공() {
        paymentService.applyResult(1L, PaymentResult.SUCCESS);

        verify(orderUseCase).confirmOrder(1L);
        verify(orderUseCase, never()).failPayment(anyLong());
    }

    @Test
    @DisplayName("결제 실패면 재고 복원을 위해 결제 실패 반영을 요청한다")
    void 결제_실패() {
        paymentService.applyResult(1L, PaymentResult.FAILURE);

        verify(orderUseCase).failPayment(1L);
        verify(orderUseCase, never()).confirmOrder(anyLong());
    }
}
