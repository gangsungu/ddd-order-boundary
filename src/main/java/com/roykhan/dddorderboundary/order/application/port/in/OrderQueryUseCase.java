package com.roykhan.dddorderboundary.order.application.port.in;

// 입력 포트 (읽기) - 주문 조회. 쓰기 모델(Order)이 아닌 읽기 모델에서 읽는다
public interface OrderQueryUseCase {

    OrderInfo findById(long orderId);
}
