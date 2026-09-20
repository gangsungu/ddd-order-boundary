package com.roykhan.dddorderboundary.domain.stock.repository;

import com.roykhan.dddorderboundary.domain.stock.Stock;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    List<Stock> findAllByProductIdIn(List<Long> productIds);
}
