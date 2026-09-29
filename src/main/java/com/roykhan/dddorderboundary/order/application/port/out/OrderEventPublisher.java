package com.roykhan.dddorderboundary.order.application.port.out;

import com.roykhan.dddorderboundary.order.domain.event.OrderEvent;

// 출력 포트 - 주문 이벤트를 내보낸다. 누가 받는지(지금은 주문 읽기 모델)는 주문 서비스가 모른다
public interface OrderEventPublisher {

    void publish(OrderEvent event);
}
