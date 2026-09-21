package com.roykhan.dddorderboundary.product.application.port.out;

import com.roykhan.dddorderboundary.product.domain.model.Stock;
import java.util.Optional;

// 출력 포트 - 재고 저장소
public interface StockRepository {

    Stock save(Stock stock);

    Optional<Stock> findByProductId(Long productId);
}
