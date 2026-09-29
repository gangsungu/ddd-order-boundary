package com.roykhan.dddorderboundary.order.domain.event;

import java.time.LocalDateTime;

// 주문이 발행하는 이벤트의 계약 - 주문 컨텍스트가 소유한다.
// 주문의 상태가 바뀔 때마다 하나씩 나가고, 주문 읽기 모델(CQRS)은 이 이벤트만 보고 갱신된다
public sealed interface OrderEvent
    permits OrderPlaced, OrderConfirmed, OrderCancelled, OrderPaymentFailed, OrderExpired {

    long orderId();

    LocalDateTime occurredAt();
}
