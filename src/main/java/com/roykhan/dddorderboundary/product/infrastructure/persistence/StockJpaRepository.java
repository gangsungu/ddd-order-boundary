package com.roykhan.dddorderboundary.product.infrastructure.persistence;

import com.roykhan.dddorderboundary.product.domain.model.Stock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockJpaRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductId(Long productId);
}
