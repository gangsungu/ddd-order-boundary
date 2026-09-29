package com.roykhan.dddorderboundary.order.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

// 입력 포트 (쓰기) - 주문의 상태를 바꾸는 유스케이스.
// 컨트롤러·스케줄러·결제 이벤트 리스너는 이 인터페이스로만 주문을 바꾼다. 조회는 OrderQueryUseCase 가 맡는다
public interface OrderCommandUseCase {

    // 주문을 만들고 항목의 재고를 예약한다. 부여된 주문 ID 를 돌려준다
    long createOrder(CreateOrderCommand command);

    // 사용자 취소 - 예약한 재고를 돌려준다
    void cancelOrder(long orderId);

    // 결제 성공 - 예약을 확정한다
    void confirmOrder(long orderId);

    // 결제 실패 - 예약한 재고를 돌려준다
    void failPayment(long orderId);

    // 결제 마감이 지난 PENDING 주문 ID 를 마감이 이른 순으로 최대 limit 건.
    // 만료라는 쓰기를 하려고 쓰기 모델을 읽는 것이라 조회 쪽이 아닌 여기에 둔다
    List<Long> findExpiredOrderIds(LocalDateTime now, int limit);

    // 결제 마감 경과 - 예약한 재고를 돌려준다
    void expireOrder(long orderId);
}
