package com.roykhan.dddorderboundary.order.adapter.out.persistence.view;

import com.roykhan.dddorderboundary.order.application.port.in.OrderInfo;
import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// 주문 읽기 모델의 저장 형태. 조회 응답(OrderInfo)과 1:1 로 대응하고 도메인 규칙은 없다.
// 쓰기 모델(orders.orders)과 테이블을 나눠, 조회 모양이 바뀌어도 쓰기 모델은 바뀌지 않는다
@Entity
@Table(name = "order_view", schema = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderViewEntity {

    // 식별자는 쓰기 모델의 주문 ID 를 그대로 쓴다
    @Id
    private Long orderId;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Column
    private LocalDateTime confirmedAt;

    @Column
    private LocalDateTime cancelledAt;

    // 조회할 때 항상 함께 내보내므로 즉시 읽는다
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_view_items", schema = "orders", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_no")
    private List<OrderViewItem> items = new ArrayList<>();

    static OrderViewEntity from(OrderInfo info) {
        OrderViewEntity entity = new OrderViewEntity();
        entity.orderId = info.id();
        entity.memberId = info.memberId();
        entity.orderStatus = info.orderStatus();
        entity.totalPrice = info.totalPrice();
        entity.expireAt = info.expireAt();
        entity.confirmedAt = info.confirmedAt();
        entity.cancelledAt = info.cancelledAt();
        entity.items = new ArrayList<>(info.items().stream().map(OrderViewItem::from).toList());
        return entity;
    }

    OrderInfo toInfo() {
        return new OrderInfo(
            orderId,
            memberId,
            orderStatus,
            totalPrice,
            expireAt,
            confirmedAt,
            cancelledAt,
            items.stream().map(OrderViewItem::toInfo).toList()
        );
    }
}
