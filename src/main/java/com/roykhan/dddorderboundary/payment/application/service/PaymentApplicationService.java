package com.roykhan.dddorderboundary.payment.application.service;

import com.roykhan.dddorderboundary.payment.application.port.in.PaymentUseCase;
import com.roykhan.dddorderboundary.payment.application.port.out.PaymentEventPublisher;
import com.roykhan.dddorderboundary.payment.domain.event.PaymentFailed;
import com.roykhan.dddorderboundary.payment.domain.event.PaymentSucceeded;
import com.roykhan.dddorderboundary.payment.domain.model.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 목업 결제 - PG 연동 없이 성공·실패 결과만 받아 이벤트로 발행한다.
// 결제는 주문을 부르지 않는다. 결제 결과를 주문이 어떻게 반영할지는 이벤트를 받는 주문이 정한다.
// 섹션 4 에서 실제 PG 연동으로 교체한다
@Service
@RequiredArgsConstructor
public class PaymentApplicationService implements PaymentUseCase {

    private final PaymentEventPublisher eventPublisher;

    // 받는 쪽은 이 트랜잭션이 커밋된 뒤에 이벤트를 받는다.
    // 아직 결제가 저장하는 상태가 없어 트랜잭션은 비어 있지만, 결제 기록이 생기면 "기록이 커밋된 결제만 알린다"가 된다
    @Override
    @Transactional
    public void applyResult(long orderId, PaymentResult result) {
        switch (result) {
            case SUCCESS -> eventPublisher.publish(PaymentSucceeded.of(orderId));
            case FAILURE -> eventPublisher.publish(PaymentFailed.of(orderId));
        }
    }
}
