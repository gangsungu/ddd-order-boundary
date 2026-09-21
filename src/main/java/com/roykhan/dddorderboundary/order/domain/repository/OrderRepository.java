package com.roykhan.dddorderboundary.order.domain.repository;

import com.roykhan.dddorderboundary.order.domain.model.Order;
import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 출력 포트 - 주문 저장소. 구현은 infrastructure/persistence 의 어댑터가 맡는다
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    // 만료 대상 - 주어진 상태에서 결제 마감이 지난 주문을 마감이 이른 순으로 최대 limit 건.
    // 스케줄러가 주문마다 트랜잭션을 따로 잡으므로 엔티티가 아닌 ID 만 읽는다
    List<Long> findIdsByStatusAndExpireAtBefore(OrderStatus status, LocalDateTime now, int limit);
}
