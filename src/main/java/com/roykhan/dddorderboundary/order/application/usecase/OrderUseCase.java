package com.roykhan.dddorderboundary.order.application.usecase;

import com.roykhan.dddorderboundary.order.application.dto.CreateOrderCommand;
import com.roykhan.dddorderboundary.order.application.dto.OrderInfo;
import java.time.LocalDateTime;
import java.util.List;

// 입력 포트 - 컨트롤러·스케줄러와 다른 컨텍스트(결제)는 이 인터페이스로만 주문을 다룬다
public interface OrderUseCase {

    // 주문을 만들고 항목의 재고를 예약한다. 부여된 주문 ID 를 돌려준다
    long createOrder(CreateOrderCommand command);

    OrderInfo findById(long orderId);

    // 사용자 취소 - 예약한 재고를 돌려준다
    void cancelOrder(long orderId);

    // 결제 성공 - 예약을 확정한다
    void confirmOrder(long orderId);

    // 결제 실패 - 예약한 재고를 돌려준다
    void failPayment(long orderId);

    // 결제 마감이 지난 PENDING 주문 ID 를 마감이 이른 순으로 최대 limit 건
    List<Long> findExpiredOrderIds(LocalDateTime now, int limit);

    // 결제 마감 경과 - 예약한 재고를 돌려준다
    void expireOrder(long orderId);
}
