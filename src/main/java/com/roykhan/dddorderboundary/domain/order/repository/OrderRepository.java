package com.roykhan.dddorderboundary.domain.order.repository;

import com.roykhan.dddorderboundary.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

}
