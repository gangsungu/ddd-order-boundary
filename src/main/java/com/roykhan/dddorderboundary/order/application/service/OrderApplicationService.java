package com.roykhan.dddorderboundary.order.application.service;

import com.roykhan.dddorderboundary.order.application.dto.CreateOrderCommand;
import com.roykhan.dddorderboundary.order.application.dto.OrderInfo;
import com.roykhan.dddorderboundary.order.application.port.ProductPort;
import com.roykhan.dddorderboundary.order.application.port.ProductSnapshot;
import com.roykhan.dddorderboundary.order.application.port.StockLine;
import com.roykhan.dddorderboundary.order.application.port.StockPort;
import com.roykhan.dddorderboundary.order.application.usecase.OrderUseCase;
import com.roykhan.dddorderboundary.order.domain.exception.OrderErrorCode;
import com.roykhan.dddorderboundary.order.domain.model.Order;
import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import com.roykhan.dddorderboundary.order.domain.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderApplicationService implements OrderUseCase {

    private final OrderRepository orderRepository;

    // 상품·재고는 주문 쪽 출력 포트로만 부른다. 상품 컨텍스트를 어떻게 부르는지는 어댑터가 안다
    private final ProductPort productPort;
    private final StockPort stockPort;

    @Value("${order.reservation.expire-time:10}")
    private int expireTime;

    @Override
    @Transactional
    public long createOrder(CreateOrderCommand command) {
        List<Long> productIds = command.lines().stream()
            .map(CreateOrderCommand.Line::productId)
            .distinct()
            .toList();

        Map<Long, ProductSnapshot> products = productPort.findAll(productIds).stream()
            .collect(Collectors.toMap(ProductSnapshot::productId, Function.identity()));

        if(products.size() != productIds.size()) {
            throw OrderErrorCode.INVALID_ORDER_ITEM.exception();
        }

        // 주문 생성 - 항목마다 주문 시점의 상품명·단가를 복사해 담고 총액은 주문이 계산한다
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(expireTime);
        Order order = Order.create(command.memberId(), expireAt);

        for(CreateOrderCommand.Line line : command.lines()) {
            ProductSnapshot product = products.get(line.productId());
            order.addItem(product.productId(), product.name(), product.price(), line.quantity());
        }

        orderRepository.save(order);

        // 재고 예약 - 예약은 주문 ID 로 묶이므로 주문을 먼저 저장해 ID 를 받는다
        List<StockLine> lines = command.lines().stream()
            .map(line -> new StockLine(line.productId(), line.quantity()))
            .toList();
        stockPort.reserve(order.getId(), lines, expireAt);

        return order.getId();
    }

    // 항목까지 트랜잭션 안에서 DTO 로 옮긴다 (open-in-view: false)
    @Override
    @Transactional(readOnly = true)
    public OrderInfo findById(long orderId) {
        return OrderInfo.from(getOrder(orderId));
    }

    @Override
    @Transactional
    public void cancelOrder(long orderId) {
        Order order = getOrder(orderId);

        // 취소 가능한 상태인지는 Order 가 판단한다
        order.cancel();
        stockPort.cancel(orderId);

        // 연관된 PENDING 상태 결제도 취소 처리
        // 추후 작업 부분
    }

    // 결제 성공 - 예약을 확정하고 주문을 확정한다
    @Override
    @Transactional
    public void confirmOrder(long orderId) {
        getOrder(orderId).confirm();
        stockPort.confirm(orderId);
    }

    // 결제 실패 - 예약을 해제해 재고를 복원한다
    @Override
    @Transactional
    public void failPayment(long orderId) {
        getOrder(orderId).failPayment();
        stockPort.cancel(orderId);
    }

    // 결제 마감이 지난 PENDING 주문 ID
    @Override
    @Transactional(readOnly = true)
    public List<Long> findExpiredOrderIds(LocalDateTime now, int limit) {
        return orderRepository.findIdsByStatusAndExpireAtBefore(OrderStatus.PENDING, now, limit);
    }

    // 주문 만료 - 예약을 해제해 재고를 복원한다
    // 스케줄러가 주문마다 따로 호출해 한 건의 실패가 다른 주문의 만료를 되돌리지 않게 한다
    @Override
    @Transactional
    public void expireOrder(long orderId) {
        getOrder(orderId).expire();
        stockPort.expire(orderId);
    }

    private Order getOrder(long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(OrderErrorCode.ORDER_NOT_FOUND::exception);
    }
}
