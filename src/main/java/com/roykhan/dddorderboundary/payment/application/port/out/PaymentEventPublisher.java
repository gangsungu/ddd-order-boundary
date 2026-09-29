package com.roykhan.dddorderboundary.payment.application.port.out;

import com.roykhan.dddorderboundary.payment.domain.event.PaymentEvent;

// 출력 포트 - 결제 이벤트를 내보낸다. 누가 받는지는 결제가 모른다.
// 지금은 스프링 인프로세스 이벤트, 섹션 4 에서 Kafka 로 바뀌어도 이 포트는 그대로다
public interface PaymentEventPublisher {

    void publish(PaymentEvent event);
}
