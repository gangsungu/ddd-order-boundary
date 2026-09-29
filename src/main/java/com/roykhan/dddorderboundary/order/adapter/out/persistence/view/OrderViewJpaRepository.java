package com.roykhan.dddorderboundary.order.adapter.out.persistence.view;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderViewJpaRepository extends JpaRepository<OrderViewEntity, Long> {
}
