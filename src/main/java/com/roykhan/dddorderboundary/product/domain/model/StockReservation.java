package com.roykhan.dddorderboundary.product.domain.model;

import com.roykhan.dddorderboundary.common.domain.BaseEntity;
import com.roykhan.dddorderboundary.product.domain.exception.ReservationErrorCode;
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

@Entity
@Getter
@Table(name = "stock_reservation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservation extends BaseEntity {

    // 예약 주체는 다른 컨텍스트의 주문이므로 객체가 아닌 ID 로만 참조한다
    @Column(name = "order_id", nullable = false)
    private Long orderId;

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

    public static StockReservation create(Stock stock, Long orderId, int quantity, LocalDateTime expireAt) {
        StockReservation stockReservation = new StockReservation();
        stockReservation.stock = stock;
        stockReservation.orderId = orderId;
        stockReservation.reservedQuantity = quantity;
        stockReservation.reservationStatus = ReservationStatus.RESERVED;
        stockReservation.expireAt = expireAt;

        stock.reserve(quantity);
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

    // 예약 해제 - 주문 취소나 결제 실패처럼 확정되지 않고 끝난 예약
    // 재고 입장에서는 해제 사유가 같으므로 CANCELLED 하나로 남긴다. 사유는 주문 상태가 가진다
    public void cancel() {
        release(ReservationStatus.CANCELLED);
    }

    // 예약 만료 - 결제 마감이 지나 풀린 예약
    public void expire() {
        release(ReservationStatus.EXPIRED);
    }

    public boolean isExpired() {
        return this.expireAt.isBefore(LocalDateTime.now());
    }

    private void release(ReservationStatus releasedStatus) {
        checkCancellable();
        this.reservationStatus = releasedStatus;
        this.cancelledAt = LocalDateTime.now();

        // Stock의 실제 재고 원복
        this.stock.cancel(this.reservedQuantity);
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
