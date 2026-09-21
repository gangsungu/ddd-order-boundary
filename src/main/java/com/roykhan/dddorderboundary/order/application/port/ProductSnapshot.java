package com.roykhan.dddorderboundary.order.application.port;

import java.math.BigDecimal;

// 주문 시점에 복사해 둘 상품 정보. 상품 컨텍스트의 표현(ProductInfo)과 따로 둔다
public record ProductSnapshot(
    Long productId,
    String name,
    BigDecimal price
) {}
