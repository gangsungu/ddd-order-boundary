package com.roykhan.dddorderboundary.product.application.service;

import com.roykhan.dddorderboundary.product.application.dto.StockInfo;
import com.roykhan.dddorderboundary.product.domain.exception.StockErrorCode;
import com.roykhan.dddorderboundary.product.domain.model.Stock;
import com.roykhan.dddorderboundary.product.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public StockInfo findByProductId(Long productId) {
        Stock stock = stockRepository.findByProductId(productId)
            .orElseThrow(StockErrorCode.STOCK_NOT_FOUND::exception);
        return StockInfo.from(stock);
    }
}
