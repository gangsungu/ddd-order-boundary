package com.roykhan.dddorderboundary.domain.stock.dto;

import com.roykhan.dddorderboundary.domain.stock.Stock;
import com.roykhan.dddorderboundary.domain.stock.enums.StockStatus;

public record StockInfo(
    Long productId,
    int quantity,
    int availableQuantity,
    int reservedQuantity,
    StockStatus stockStatus
) {
    public static StockInfo from(Stock stock) {
        return new StockInfo(
            stock.getProductId(),
            stock.getQuantity(),
            stock.getAvailableQuantity(),
            stock.reservedQuantity(),
            stock.getStockStatus()
        );
    }
}
