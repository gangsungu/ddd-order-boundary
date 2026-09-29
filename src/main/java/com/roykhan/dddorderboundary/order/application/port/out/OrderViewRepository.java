package com.roykhan.dddorderboundary.order.application.port.out;

import com.roykhan.dddorderboundary.order.application.port.in.OrderInfo;
import java.util.Optional;

// 출력 포트 - 주문 읽기 모델 저장소. 쓰기 모델의 OrderRepository 와 테이블을 공유하지 않는다
public interface OrderViewRepository {

    // 같은 주문 ID 가 있으면 덮어쓴다
    void save(OrderInfo view);

    Optional<OrderInfo> findById(long orderId);
}
