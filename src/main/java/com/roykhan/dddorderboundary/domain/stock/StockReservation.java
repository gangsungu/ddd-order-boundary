package com.roykhan.dddorderboundary.domain.stock;

import com.roykhan.dddorderboundary.common.exception.ReservationErrorCode;
import com.roykhan.dddorderboundary.domain.base.BaseEntity;
import com.roykhan.dddorderboundary.domain.order.Order;
import com.roykhan.dddorderboundary.domain.stock.enums.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

@Entity
@Getter
@Table(name = "stock_reservation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private int reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus reservationStatus;

    @Column
    private LocalDateTime expireAt;

    @Column
    private LocalDateTime confirmedAt;

    @Column
    private LocalDateTime cancelledAt;

    public static StockReservation create(Stock stock, Order order, int quantity, LocalDateTime expireAt) {
        StockReservation stockReservation = new StockReservation();
        stockReservation.stock = stock;
        stockReservation.order = order;
        stockReservation.reservedQuantity = quantity;
        stockReservation.reservationStatus = ReservationStatus.RESERVED;
        stockReservation.expireAt = expireAt;

        stock.reserve(quantity);
        order.addReservation(stockReservation);
        return stockReservation;
    }

    // 재고 예약 확인
    public void confirm() {
        checkConfirm();
        this.reservationStatus = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();

        // Stock의 실제 재고 차감
        this.stock.confirm(this.reservedQuantity);
    }

    // 사용자 주문 취소 - 재고 예약 해제 처리
    public void cancelByUser() {
        checkCancellable();

        this.reservationStatus = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();

        // Stock의 실제 재고 원복
        this.stock.cancel(this.reservedQuantity);
    }

    // 주문 만료에 의한 주문 취소 - 재고 예약 해제 처리
    public void cancelByExpiration() {
        checkCancellable();
        this.reservationStatus = ReservationStatus.EXPIRED;
        this.cancelledAt = LocalDateTime.now();

        // Stock의 실제 재고 원복
        this.stock.cancel(this.reservedQuantity);
    }

    public boolean isExpired() {
        return this.expireAt.isBefore(LocalDateTime.now());
    }

    // 재고 예약 해제 확인 - 이미 확정되었거나 해제된 예약을 다시 해제하면 가용수량이 중복 복원된다
    private void checkCancellable() {
        if(this.reservationStatus != ReservationStatus.RESERVED) {
            throw ReservationErrorCode.RESERVATION_NOT_CANCELLABLE.exception();
        }
    }

    private void checkConfirm() {
        if(this.reservationStatus != ReservationStatus.RESERVED) {
            throw ReservationErrorCode.RESERVATION_NOT_CONFIRMABLE.exception();
        }

        if(isExpired()) {
            throw ReservationErrorCode.RESERVATION_EXPIRED.exception();
        }
    }
}
