package com.roykhan.dddorderboundary.domain.stock.service;

import com.roykhan.dddorderboundary.common.exception.StockErrorCode;
import com.roykhan.dddorderboundary.domain.stock.Stock;
import com.roykhan.dddorderboundary.domain.stock.dto.StockInfo;
import com.roykhan.dddorderboundary.domain.stock.repository.StockRepository;
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
