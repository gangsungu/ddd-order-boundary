package com.roykhan.dddorderboundary.product.application.dto;

import java.time.LocalDateTime;
import java.util.List;

// 한 주문의 항목들을 같은 결제 마감으로 한 번에 예약한다
public record ReserveStockCommand(
    Long orderId,
    List<Line> lines,
    LocalDateTime expireAt
) {
    public record Line(
        Long productId,
        int quantity
    ) {}
}
