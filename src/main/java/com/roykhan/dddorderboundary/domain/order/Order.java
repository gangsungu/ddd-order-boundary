package com.roykhan.dddorderboundary.domain.order;

import com.roykhan.dddorderboundary.common.exception.OrderErrorCode;
import com.roykhan.dddorderboundary.domain.base.BaseEntity;
import com.roykhan.dddorderboundary.domain.order.enums.OrderStatus;
import com.roykhan.dddorderboundary.domain.stock.StockReservation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Column(nullable = false)
    private Long memberId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<StockReservation> reservations = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Column
    private LocalDateTime confirmedAt;

    @Column
    private LocalDateTime cancelledAt;

    public static Order create(Long memberId, LocalDateTime expireAt) {
        Order order = new Order();
        order.memberId = memberId;
        order.orderStatus = OrderStatus.PENDING;
        order.expireAt = expireAt;
        order.totalPrice = BigDecimal.ZERO;
        return order;
    }

    // 주문 시점의 상품명·단가를 복사해 항목으로 담는다.
    // 총액이 항목 합과 어긋날 수 없도록 갱신은 이 메서드에서만 한다.
    public void addItem(Long productId, String productName, BigDecimal unitPrice, int quantity) {
        OrderItem orderItem = OrderItem.create(this, productId, productName, unitPrice, quantity);
        this.items.add(orderItem);
        this.totalPrice = this.totalPrice.add(orderItem.amount());
    }

    public void addReservation(StockReservation reservation) {
        this.reservations.add(reservation);
    }

    // 밖에서 항목을 넣으면 총액과 어긋나므로 읽기 전용으로 내보낸다
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(this.items);
    }

    // 주문 취소
    // 점유한 재고 반환
    public void cancel() {
        checkPending();
        this.orderStatus = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();

        reservations.forEach(StockReservation::cancelByUser);
    }

    // 결제 성공 - 주문 확정
    // 예약을 확정해 총 재고를 차감한다
    public void confirm() {
        checkPending();
        this.orderStatus = OrderStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();

        reservations.forEach(StockReservation::confirm);
    }

    // 결제 실패 - 보상 트랜잭션
    // 점유한 재고 반환
    public void failPayment() {
        checkPending();
        this.orderStatus = OrderStatus.PAYMENT_FAILED;
        this.cancelledAt = LocalDateTime.now();

        reservations.forEach(StockReservation::cancelByPaymentFailure);
    }

    // 주문 만료
    // 점유한 재고 반환
    public void expire() {
        this.orderStatus = OrderStatus.EXPIRED;
        this.expireAt = LocalDateTime.now();
        this.cancelledAt = LocalDateTime.now();

        reservations.forEach(StockReservation::cancelByExpiration);
    }

    // 취소와 결제 결과 반영은 결제 대기 중인 주문에서만 가능하다.
    // 확정된 주문의 취소는 환불이라 결제 취소가 선행되어야 하고, 이는 섹션 3 범위다.
    private void checkPending() {
        if(this.orderStatus == OrderStatus.CANCELLED) {
            throw OrderErrorCode.ORDER_ALREADY_CANCELLED.exception();
        }

        if(this.orderStatus == OrderStatus.EXPIRED) {
            throw OrderErrorCode.ORDER_ALREADY_EXPIRED.exception();
        }

        if(this.orderStatus == OrderStatus.CONFIRMED) {
            throw OrderErrorCode.ORDER_ALREADY_CONFIRMED.exception();
        }

        if(this.orderStatus == OrderStatus.PAYMENT_FAILED) {
            throw OrderErrorCode.ORDER_PAYMENT_FAILED.exception();
        }
    }
}
