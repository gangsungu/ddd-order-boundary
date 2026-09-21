package com.roykhan.dddorderboundary.domain.order.repository;

import com.roykhan.dddorderboundary.domain.order.Order;
import com.roykhan.dddorderboundary.domain.order.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 만료 대상 - 주어진 상태에서 결제 마감이 지난 주문을 마감이 이른 순으로.
    // 스케줄러가 주문마다 트랜잭션을 따로 잡으므로 엔티티가 아닌 ID 만 읽는다
    @Query("select o.id from Order o where o.orderStatus = :status and o.expireAt < :now order by o.expireAt")
    List<Long> findIdsByStatusAndExpireAtBefore(OrderStatus status, LocalDateTime now, Pageable pageable);
}
