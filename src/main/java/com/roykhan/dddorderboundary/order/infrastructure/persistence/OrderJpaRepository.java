package com.roykhan.dddorderboundary.order.infrastructure.persistence;

import com.roykhan.dddorderboundary.order.domain.model.Order;
import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    @Query("select o.id from Order o where o.orderStatus = :status and o.expireAt < :now order by o.expireAt")
    List<Long> findIdsByStatusAndExpireAtBefore(OrderStatus status, LocalDateTime now, Pageable pageable);
}
