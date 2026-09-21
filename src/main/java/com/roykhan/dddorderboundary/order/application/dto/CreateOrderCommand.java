package com.roykhan.dddorderboundary.order.application.dto;

import java.util.List;

public record CreateOrderCommand(
    Long memberId,
    List<Line> lines
) {
    public record Line(
        Long productId,
        int quantity
    ) {}
}
