package com.roykhan.dddorderboundary.product.domain.model;

import com.roykhan.dddorderboundary.common.domain.BaseEntity;
import com.roykhan.dddorderboundary.product.domain.exception.StockErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "stocks", schema = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int availableQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockStatus stockStatus;

    @Version
    private Long version;

    public static Stock create(Long productId, int quantity) {
        Stock stock = new Stock();
        stock.productId = productId;
        stock.quantity = quantity;
        stock.availableQuantity = quantity;
        stock.updateStatus();
        return stock;
    }

    // 주문 예약
    public void reserve(int reserveQuantity) {
        checkReservation(reserveQuantity);
        this.availableQuantity -= reserveQuantity;
        updateStatus();
    }

    // 주문 취소
    public void cancel(int reserveQuantity) {
        this.availableQuantity += reserveQuantity;
        updateStatus();
    }

    // 주문 확정
    public void confirm(int reserveQuantity) {
        checkConfirm(reserveQuantity);

        this.quantity -= reserveQuantity;
        updateStatus();
    }

    // 예약 수량 - 가용 수량으로 넘어가지 않고 확정을 기다리는 수량
    // 예약은 가용 수량만 줄이고 확정이 총 재고를 줄이므로 둘의 차이가 곧 예약 중인 수량이다
    public int reservedQuantity() {
        return this.quantity - this.availableQuantity;
    }

    // 재고상태 자동 업데이트
    private void updateStatus() {
        if(this.availableQuantity > 0) {
            this.stockStatus = StockStatus.IN_STOCK;
        }
        else {
            this.stockStatus = StockStatus.SOLD_OUT;
        }
    }

    // 주문 예약 확인
    private void checkReservation(int reserveQuantity) {
        if(reserveQuantity <= 0) {
            throw StockErrorCode.INVALID_QUANTITY.exception();
        }

        if(this.availableQuantity < reserveQuantity) {
            throw StockErrorCode.OUT_OF_STOCK.exception();
        }
    }

    // 주문 확정 확인 - 가용수량은 예약 시점에 이미 차감되었으므로 총 재고만 확인한다
    private void checkConfirm(int reserveQuantity) {
        if(reserveQuantity <= 0) {
            throw StockErrorCode.INVALID_QUANTITY.exception();
        }

        if(this.quantity < reserveQuantity) {
            throw StockErrorCode.OUT_OF_STOCK.exception();
        }
    }
}
