package com.roykhan.dddorderboundary.domain.payment.service;

import com.roykhan.dddorderboundary.domain.order.service.OrderService;
import com.roykhan.dddorderboundary.domain.payment.enums.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 목업 결제 - PG 연동 없이 성공·실패 결과만 받아 주문에 알린다.
// 주문은 결제 결과의 표현(PaymentResult)을 몰라도 되도록 여기서 확정·실패 호출로 바꿔 넘긴다.
// 섹션 3 에서 실제 PG 연동으로 교체한다
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderService orderService;

    public void applyResult(long orderId, PaymentResult result) {
        switch (result) {
            case SUCCESS -> orderService.confirmOrder(orderId);
            case FAILURE -> orderService.failPayment(orderId);
        }
    }
}
