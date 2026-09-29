package com.roykhan.dddorderboundary.order.adapter.out.persistence.view;

import com.roykhan.dddorderboundary.order.application.port.in.OrderInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// 읽기 모델의 항목. 금액까지 계산해 저장해 두어 조회할 때 다시 계산하지 않는다
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderViewItem {

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false, scale = 2, precision = 15)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, scale = 2, precision = 15)
    private BigDecimal amount;

    static OrderViewItem from(OrderInfo.Item item) {
        OrderViewItem viewItem = new OrderViewItem();
        viewItem.productId = item.productId();
        viewItem.productName = item.productName();
        viewItem.unitPrice = item.unitPrice();
        viewItem.quantity = item.quantity();
        viewItem.amount = item.amount();
        return viewItem;
    }

    OrderInfo.Item toInfo() {
        return new OrderInfo.Item(productId, productName, unitPrice, quantity, amount);
    }
}
