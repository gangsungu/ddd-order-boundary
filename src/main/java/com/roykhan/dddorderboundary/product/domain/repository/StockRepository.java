package com.roykhan.dddorderboundary.product.domain.repository;

import com.roykhan.dddorderboundary.product.domain.model.Stock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductId(Long productId);
}
