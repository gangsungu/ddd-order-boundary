package com.roykhan.dddorderboundary.order.application.port.out;

public record StockLine(
    Long productId,
    int quantity
) {}
