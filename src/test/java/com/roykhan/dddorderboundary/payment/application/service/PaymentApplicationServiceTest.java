package com.roykhan.dddorderboundary.payment.application.service;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.roykhan.dddorderboundary.payment.application.port.OrderPort;
import com.roykhan.dddorderboundary.payment.domain.model.PaymentResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 결제 결과를 어떤 알림으로 바꾸는지만 본다. 알림이 주문의 어떤 호출로 이어지는지는 OrderAdapterTest 가 확인한다
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentApplicationService 단위 테스트")
class PaymentApplicationServiceTest {

    @Mock
    private OrderPort orderPort;

    @InjectMocks
    private PaymentApplicationService paymentService;

    @Test
    @DisplayName("결제 성공이면 주문에 결제 성공을 알린다")
    void 결제_성공() {
        paymentService.applyResult(1L, PaymentResult.SUCCESS);

        verify(orderPort).notifyPaymentSucceeded(1L);
        verify(orderPort, never()).notifyPaymentFailed(anyLong());
    }

    @Test
    @DisplayName("결제 실패면 주문에 결제 실패를 알린다")
    void 결제_실패() {
        paymentService.applyResult(1L, PaymentResult.FAILURE);

        verify(orderPort).notifyPaymentFailed(1L);
        verify(orderPort, never()).notifyPaymentSucceeded(anyLong());
    }
}
