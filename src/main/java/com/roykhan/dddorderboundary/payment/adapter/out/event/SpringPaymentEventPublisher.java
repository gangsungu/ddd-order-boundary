package com.roykhan.dddorderboundary.payment.adapter.out.event;

import com.roykhan.dddorderboundary.payment.application.port.out.PaymentEventPublisher;
import com.roykhan.dddorderboundary.payment.domain.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// 출력 어댑터 - 결제 이벤트를 스프링 인프로세스 이벤트로 발행한다.
// 받는 쪽은 결제 트랜잭션이 커밋된 뒤에 받는다(@TransactionalEventListener).
// 섹션 4 에서 Kafka 로 옮기면 이 어댑터만 교체한다
@Component
@RequiredArgsConstructor
public class SpringPaymentEventPublisher implements PaymentEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(PaymentEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
