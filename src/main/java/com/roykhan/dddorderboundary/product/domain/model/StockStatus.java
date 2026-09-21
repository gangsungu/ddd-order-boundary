package com.roykhan.dddorderboundary.product.domain.model;

import lombok.Getter;

@Getter
public enum StockStatus {
    SOLD_OUT("품절"),
    IN_STOCK("재고 있음");

    private final String description;

    StockStatus(final String description) {
        this.description = description;
    }
}
