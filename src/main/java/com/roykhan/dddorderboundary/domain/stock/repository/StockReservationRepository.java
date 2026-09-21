package com.roykhan.dddorderboundary.domain.stock.repository;

import com.roykhan.dddorderboundary.domain.stock.StockReservation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findAllByOrderId(Long orderId);
}
