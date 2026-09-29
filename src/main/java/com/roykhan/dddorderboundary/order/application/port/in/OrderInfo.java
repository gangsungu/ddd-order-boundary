package com.roykhan.dddorderboundary.order.application.port.in;

import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// 주문 읽기 모델 - 조회 응답의 모양 그대로 저장해 두고 그대로 돌려준다.
// 쓰기 모델(Order)에서 만들지 않는다. 주문 이벤트를 받아 OrderProjectionService 가 채운다
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
    ) {}

    public OrderInfo confirmed(LocalDateTime confirmedAt) {
        return new OrderInfo(id, memberId, OrderStatus.CONFIRMED, totalPrice, expireAt, confirmedAt, cancelledAt, items);
    }

    // 사용자 취소·결제 실패·만료는 모두 풀린 시각을 cancelledAt 에 남긴다 (쓰기 모델과 같은 규칙)
    public OrderInfo released(OrderStatus status, LocalDateTime cancelledAt) {
        return new OrderInfo(id, memberId, status, totalPrice, expireAt, confirmedAt, cancelledAt, items);
    }
}
