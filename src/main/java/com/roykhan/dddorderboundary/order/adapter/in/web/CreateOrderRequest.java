package com.roykhan.dddorderboundary.order.adapter.in.web;

import com.roykhan.dddorderboundary.order.application.port.in.CreateOrderCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record CreateOrderRequest(
    @NotNull(message = "회원 ID는 필수입니다.")
    Long memberId,

    @NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다.")
    @Valid
    List<OrderLine> items
) {
    public record OrderLine(
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,

        @Positive(message = "수량은 1개 이상이어야 합니다.")
        int quantity
    ) {}

    public CreateOrderCommand toCommand() {
        List<CreateOrderCommand.Line> lines = items.stream()
            .map(item -> new CreateOrderCommand.Line(item.productId(), item.quantity()))
            .toList();
        return new CreateOrderCommand(memberId, lines);
    }
}
