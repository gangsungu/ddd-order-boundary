package com.roykhan.dddorderboundary.product.infrastructure.persistence;

import com.roykhan.dddorderboundary.product.domain.model.StockReservation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationJpaRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findAllByOrderId(Long orderId);
}
