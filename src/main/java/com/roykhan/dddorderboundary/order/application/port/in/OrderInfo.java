package com.roykhan.dddorderboundary.order.application.port.in;

import com.roykhan.dddorderboundary.order.domain.model.Order;
import com.roykhan.dddorderboundary.order.domain.model.OrderItem;
import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderInfo(
    Long id,
    Long memberId,
    OrderStatus orderStatus,
    BigDecimal totalPrice,
    LocalDateTime expireAt,
    LocalDateTime confirmedAt,
    LocalDateTime cancelledAt,
    List<Item> items
) {
    // 항목은 주문 시점에 복사해 둔 값이므로 상품을 다시 조회하지 않는다
    public record Item(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal amount
    ) {
        public static Item from(OrderItem orderItem) {
            return new Item(
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity(),
                orderItem.amount()
            );
        }
    }

    public static OrderInfo from(Order order) {
        return new OrderInfo(
            order.getId(),
            order.getMemberId(),
            order.getOrderStatus(),
            order.getTotalPrice(),
            order.getExpireAt(),
            order.getConfirmedAt(),
            order.getCancelledAt(),
            order.getItems().stream().map(Item::from).toList()
        );
    }
}
