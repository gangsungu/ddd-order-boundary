package com.roykhan.dddorderboundary.domain.product;

import lombok.Getter;

@Getter
public enum StockStatus {
    SOLD_OUT("품절"),
    PROCESSING("주문 중"),
    IN_STOCK("재고있음");

    private final String value;

    StockStatus(String value) {
        this.value = value;
    }
}
