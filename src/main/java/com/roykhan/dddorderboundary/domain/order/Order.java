package com.roykhan.dddorderboundary.domain.order;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Column(nullable = false)
    private Long memberId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<StockReservation> reservations = new ArrayList();

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

    public static Order create(Long memberId, BigDecimal totalPrice, LocalDateTime expireTime) {
        Order order = new Order();
        order.memberId = memberId;
        order.totalPrice = totalPrice;
        order.orderStatus = OrderStatus.PENDING;
        order.expireAt = expireTime;
        return order;
    }

    public void addReservation(StockReservation reservation) {
        this.reservations.add(reservation);
    }
}
