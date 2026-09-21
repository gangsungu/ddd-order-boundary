package com.roykhan.dddorderboundary.order.application.port;

public record StockLine(
    Long productId,
    int quantity
) {}
