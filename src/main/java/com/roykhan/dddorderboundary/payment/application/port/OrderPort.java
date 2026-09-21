package com.roykhan.dddorderboundary.payment.application.port;

// 출력 포트 - 결제 결과를 주문에 알린다.
// 결제는 "주문을 확정한다"가 아니라 "결제가 성공했다"만 말하고, 그 결과로 주문이 무엇을 할지는 주문이 정한다
public interface OrderPort {

    void notifyPaymentSucceeded(long orderId);

    void notifyPaymentFailed(long orderId);
}
