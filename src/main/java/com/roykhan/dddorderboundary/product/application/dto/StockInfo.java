package com.roykhan.dddorderboundary.product.application.dto;

import com.roykhan.dddorderboundary.product.domain.model.Stock;
import com.roykhan.dddorderboundary.product.domain.model.StockStatus;

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
