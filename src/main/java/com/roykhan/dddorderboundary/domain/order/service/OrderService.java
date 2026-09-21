package com.roykhan.dddorderboundary.domain.order.service;

import com.roykhan.dddorderboundary.common.exception.OrderErrorCode;
import com.roykhan.dddorderboundary.domain.order.Order;
import com.roykhan.dddorderboundary.domain.order.dto.CreateOrderRequest;
import com.roykhan.dddorderboundary.domain.order.dto.OrderInfo;
import com.roykhan.dddorderboundary.domain.order.enums.OrderStatus;
import com.roykhan.dddorderboundary.domain.order.repository.OrderRepository;
import com.roykhan.dddorderboundary.domain.product.Product;
import com.roykhan.dddorderboundary.domain.product.repository.ProductRepository;
import com.roykhan.dddorderboundary.domain.stock.Stock;
import com.roykhan.dddorderboundary.domain.stock.repository.StockRepository;
import com.roykhan.dddorderboundary.domain.stock.service.StockReservationService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;

    private final StockReservationService stockReservationService;

    @Value("${order.reservation.expire-time:10}")
    private int expireTime;

    @Transactional
    public long createOrder(@Valid CreateOrderRequest request) {
        List<Long> productIds = request.items().stream()
            .map(CreateOrderRequest.OrderLine::productId)
            .distinct()
            .toList();

        Map<Long, Product> products = productRepository.findAllById(productIds).stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));

        Map<Long, Stock> stocks = stockRepository.findAllByProductIdIn(productIds).stream()
            .collect(Collectors.toMap(Stock::getProductId, Function.identity()));

        if(products.size() != productIds.size() || stocks.size() != productIds.size()) {
            throw OrderErrorCode.INVALID_ORDER_ITEM.exception();
        }

        // 주문 생성 - 항목마다 주문 시점의 상품명·단가를 복사해 담고 총액은 주문이 계산한다
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(expireTime);
        Order order = Order.create(request.memberId(), expireAt);

        for(CreateOrderRequest.OrderLine line : request.items()) {
            Product product = products.get(line.productId());
            order.addItem(product.getId(), product.getName(), product.getPrice(), line.quantity());
        }

        orderRepository.save(order);

        // 재고 예약
        for(CreateOrderRequest.OrderLine line : request.items()) {
            stockReservationService.reserveStock(order, stocks.get(line.productId()), line.quantity(), expireAt);
        }

        return order.getId();
    }

    // 항목까지 트랜잭션 안에서 DTO 로 옮긴다 (open-in-view: false)
    @Transactional(readOnly = true)
    public OrderInfo findById(long orderId) {
        return OrderInfo.from(getOrder(orderId));
    }

    @Transactional
    public void cancelOrder(long orderId) {
        Order order = getOrder(orderId);

        // 취소 가능한 상태인지는 Order 가 판단한다
        order.cancel();

        // 연관된 PENDING 상태 결제도 취소 처리
        // 추후 작업 부분
    }

    // 결제 성공 - 예약을 확정하고 주문을 확정한다
    @Transactional
    public void confirmOrder(long orderId) {
        getOrder(orderId).confirm();
    }

    // 결제 실패 - 예약을 해제해 재고를 복원한다
    @Transactional
    public void failPayment(long orderId) {
        getOrder(orderId).failPayment();
    }

    // 결제 마감이 지난 PENDING 주문 ID
    @Transactional(readOnly = true)
    public List<Long> findExpiredOrderIds(LocalDateTime now, int limit) {
        return orderRepository.findIdsByStatusAndExpireAtBefore(OrderStatus.PENDING, now, PageRequest.of(0, limit));
    }

    // 주문 만료 - 예약을 해제해 재고를 복원한다
    // 스케줄러가 주문마다 따로 호출해 한 건의 실패가 다른 주문의 만료를 되돌리지 않게 한다
    @Transactional
    public void expireOrder(long orderId) {
        getOrder(orderId).expire();
    }

    private Order getOrder(long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(OrderErrorCode.ORDER_NOT_FOUND::exception);
    }
}
