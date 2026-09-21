package com.roykhan.dddorderboundary.product.infrastructure.persistence;

import com.roykhan.dddorderboundary.product.domain.model.Stock;
import com.roykhan.dddorderboundary.product.domain.repository.StockRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

// 출력 어댑터 - 재고 저장소 포트를 Spring Data JPA 로 구현한다
@Repository
@RequiredArgsConstructor
public class StockRepositoryAdapter implements StockRepository {

    private final StockJpaRepository stockJpaRepository;

    @Override
    public Stock save(Stock stock) {
        return stockJpaRepository.save(stock);
    }

    @Override
    public Optional<Stock> findByProductId(Long productId) {
        return stockJpaRepository.findByProductId(productId);
    }
}
