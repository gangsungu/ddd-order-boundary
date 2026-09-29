package com.roykhan.dddorderboundary.order.domain.event;

import com.roykhan.dddorderboundary.order.domain.model.Order;
import com.roykhan.dddorderboundary.order.domain.model.OrderItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// 주문 생성 - 읽기 모델이 주문을 처음 그릴 수 있도록 주문 시점의 항목까지 모두 싣는다
public record OrderPlaced(
    long orderId,
    long memberId,
    BigDecimal totalPrice,
    LocalDateTime expireAt,
    List<Item> items,
    LocalDateTime occurredAt
) implements OrderEvent {

    public record Item(
        long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity
    ) {
        static Item from(OrderItem orderItem) {
            return new Item(
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity()
            );
        }
    }

    // 식별자가 있어야 하므로 저장된 주문에서만 만든다
    public static OrderPlaced from(Order order) {
        return new OrderPlaced(
            order.getId(),
            order.getMemberId(),
            order.getTotalPrice(),
            order.getExpireAt(),
            order.getItems().stream().map(Item::from).toList(),
            LocalDateTime.now()
        );
    }
}
