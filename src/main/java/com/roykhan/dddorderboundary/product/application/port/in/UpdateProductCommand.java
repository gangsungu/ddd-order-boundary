package com.roykhan.dddorderboundary.product.application.port.in;

import java.math.BigDecimal;

// 재고 수량은 상품 수정으로 바뀌지 않으므로 담지 않는다
public record UpdateProductCommand(
    String name,
    String description,
    BigDecimal price
) {}
