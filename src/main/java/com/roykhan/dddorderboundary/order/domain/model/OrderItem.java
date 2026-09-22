package com.roykhan.dddorderboundary.order.domain.model;

import com.roykhan.dddorderboundary.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "order_items", schema = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long productId;

    // 상품명과 단가는 상품 컨텍스트가 소유하지만 주문 시점 값을 복사해 둔다.
    // 이후 상품이 바뀌어도 이미 끝난 주문의 금액은 달라지면 안 된다.
    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false, scale = 2, precision = 15)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    // 항목은 주문을 통해서만 만들어지도록 패키지 범위로 둔다
    static OrderItem create(Order order, Long productId, String productName, BigDecimal unitPrice, int quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.order = order;
        orderItem.productId = productId;
        orderItem.productName = productName;
        orderItem.unitPrice = unitPrice;
        orderItem.quantity = quantity;
        return orderItem;
    }

    public BigDecimal amount() {
        return this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
    }
}
