package com.roykhan.dddorderboundary.domain.stock.repository;

import com.roykhan.dddorderboundary.domain.stock.Stock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductId(Long productId);

    List<Stock> findAllByProductIdIn(List<Long> productIds);
}
