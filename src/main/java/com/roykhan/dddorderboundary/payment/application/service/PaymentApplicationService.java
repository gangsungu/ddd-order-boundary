package com.roykhan.dddorderboundary.payment.application.service;

import com.roykhan.dddorderboundary.payment.application.port.OrderPort;
import com.roykhan.dddorderboundary.payment.application.usecase.PaymentUseCase;
import com.roykhan.dddorderboundary.payment.domain.model.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 목업 결제 - PG 연동 없이 성공·실패 결과만 받아 주문에 알린다.
// 주문은 결제 결과의 표현(PaymentResult)을 몰라도 되도록 여기서 성공·실패 알림으로 바꿔 넘긴다.
// 섹션 3 에서 실제 PG 연동으로 교체한다
@Service
@RequiredArgsConstructor
public class PaymentApplicationService implements PaymentUseCase {

    private final OrderPort orderPort;

    @Override
    public void applyResult(long orderId, PaymentResult result) {
        switch (result) {
            case SUCCESS -> orderPort.notifyPaymentSucceeded(orderId);
            case FAILURE -> orderPort.notifyPaymentFailed(orderId);
        }
    }
}
