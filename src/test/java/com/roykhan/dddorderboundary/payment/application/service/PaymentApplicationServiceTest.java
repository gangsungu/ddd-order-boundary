package com.roykhan.dddorderboundary.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.roykhan.dddorderboundary.payment.application.port.out.PaymentEventPublisher;
import com.roykhan.dddorderboundary.payment.domain.event.PaymentEvent;
import com.roykhan.dddorderboundary.payment.domain.event.PaymentFailed;
import com.roykhan.dddorderboundary.payment.domain.event.PaymentSucceeded;
import com.roykhan.dddorderboundary.payment.domain.model.PaymentResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 결제 결과를 어떤 이벤트로 바꾸는지만 본다. 이벤트가 주문에 어떻게 반영되는지는 PaymentEventListenerTest 가 확인한다
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentApplicationService 단위 테스트")
class PaymentApplicationServiceTest {

    @Mock
    private PaymentEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<PaymentEvent> eventCaptor;

    @InjectMocks
    private PaymentApplicationService paymentService;

    @Test
    @DisplayName("결제 성공이면 PaymentSucceeded 를 발행한다")
    void 결제_성공() {
        paymentService.applyResult(1L, PaymentResult.SUCCESS);

        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(PaymentSucceeded.class);
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("결제 실패면 PaymentFailed 를 발행한다")
    void 결제_실패() {
        paymentService.applyResult(1L, PaymentResult.FAILURE);

        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(PaymentFailed.class);
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(1L);
    }
}
