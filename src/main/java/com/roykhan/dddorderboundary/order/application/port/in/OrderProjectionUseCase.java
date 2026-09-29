package com.roykhan.dddorderboundary.order.application.port.in;

import com.roykhan.dddorderboundary.order.domain.event.OrderEvent;

// 입력 포트 (읽기 모델 갱신) - 주문 이벤트 하나를 읽기 모델에 반영한다
public interface OrderProjectionUseCase {

    void project(OrderEvent event);
}
