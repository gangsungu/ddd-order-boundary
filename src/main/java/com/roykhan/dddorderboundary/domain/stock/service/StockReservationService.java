package com.roykhan.dddorderboundary.domain.stock.service;

import com.roykhan.dddorderboundary.common.exception.StockErrorCode;
import com.roykhan.dddorderboundary.domain.order.Order;
import com.roykhan.dddorderboundary.domain.stock.Stock;
import com.roykhan.dddorderboundary.domain.stock.StockReservation;
import com.roykhan.dddorderboundary.domain.stock.repository.StockRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final StockRepository stockRepository;

    public void reserveStock(Order order, Stock stock, int quantity, LocalDateTime expiredAt) {
        Stock freshStock = stockRepository.findById(stock.getId()).orElseThrow(
            StockErrorCode.STOCK_NOT_FOUND::exception);

        StockReservation.create(freshStock, order, quantity, expiredAt);
        stockRepository.save(freshStock);
    }
}