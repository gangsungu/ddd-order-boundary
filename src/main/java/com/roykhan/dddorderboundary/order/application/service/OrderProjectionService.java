package com.roykhan.dddorderboundary.order.application.service;

import com.roykhan.dddorderboundary.order.application.port.in.OrderInfo;
import com.roykhan.dddorderboundary.order.application.port.in.OrderProjectionUseCase;
import com.roykhan.dddorderboundary.order.application.port.out.OrderViewRepository;
import com.roykhan.dddorderboundary.order.domain.event.OrderCancelled;
import com.roykhan.dddorderboundary.order.domain.event.OrderConfirmed;
import com.roykhan.dddorderboundary.order.domain.event.OrderEvent;
import com.roykhan.dddorderboundary.order.domain.event.OrderExpired;
import com.roykhan.dddorderboundary.order.domain.event.OrderPaymentFailed;
import com.roykhan.dddorderboundary.order.domain.event.OrderPlaced;
import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 읽기 모델 갱신 - 주문 이벤트만 보고 읽기 모델을 그린다. 쓰기 모델(Order)을 다시 읽지 않는다.
// 상태 검사는 쓰기 쪽이 이미 끝냈으므로 여기서는 이벤트를 그대로 옮기기만 한다
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProjectionService implements OrderProjectionUseCase {

    private final OrderViewRepository orderViewRepository;

    @Override
    @Transactional
    public void project(OrderEvent event) {
        switch (event) {
            case OrderPlaced e -> orderViewRepository.save(placed(e));
            case OrderConfirmed e -> update(e, view -> view.confirmed(e.occurredAt()));
            case OrderCancelled e -> update(e, view -> view.released(OrderStatus.CANCELLED, e.occurredAt()));
            case OrderPaymentFailed e -> update(e, view -> view.released(OrderStatus.PAYMENT_FAILED, e.occurredAt()));
            case OrderExpired e -> update(e, view -> view.released(OrderStatus.EXPIRED, e.occurredAt()));
        }
    }

    private static OrderInfo placed(OrderPlaced e) {
        return new OrderInfo(
            e.orderId(),
            e.memberId(),
            OrderStatus.PENDING,
            e.totalPrice(),
            e.expireAt(),
            null,
            null,
            e.items().stream()
                .map(item -> new OrderInfo.Item(
                    item.productId(),
                    item.productName(),
                    item.unitPrice(),
                    item.quantity(),
                    item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()))))
                .toList()
        );
    }

    private void update(OrderEvent event, UnaryOperator<OrderInfo> change) {
        orderViewRepository.findById(event.orderId()).ifPresentOrElse(
            view -> orderViewRepository.save(change.apply(view)),
            // 생성 이벤트를 놓친 주문. 읽기 모델을 다시 만들어야 하므로 크게 남긴다
            () -> log.error("읽기 모델에 없는 주문의 이벤트: orderId={}, event={}",
                event.orderId(), event.getClass().getSimpleName()));
    }
}
