package com.roykhan.dddorderboundary.domain.order.service;

import com.roykhan.dddorderboundary.common.exception.OrderErrorCode;
import com.roykhan.dddorderboundary.domain.order.Order;
import com.roykhan.dddorderboundary.domain.order.dto.CreateOrderRequest;
import com.roykhan.dddorderboundary.domain.order.repository.OrderRepository;
import com.roykhan.dddorderboundary.domain.product.Product;
import com.roykhan.dddorderboundary.domain.product.repository.ProductRepository;
import com.roykhan.dddorderboundary.domain.stock.Stock;
import com.roykhan.dddorderboundary.domain.stock.repository.StockRepository;
import com.roykhan.dddorderboundary.domain.stock.service.StockReservationService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
            .map(CreateOrderRequest.OrderItem::productId)
            .distinct()
            .toList();

        Map<Long, Product> products = productRepository.findAllById(productIds).stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));

        Map<Long, Stock> stocks = stockRepository.findAllByProductIdIn(productIds).stream()
            .collect(Collectors.toMap(Stock::getProductId, Function.identity()));

        if(products.size() != productIds.size() || stocks.size() != productIds.size()) {
            throw OrderErrorCode.INVALID_ORDER_ITEM.exception();
        }

        // 총 주문 금액 계산 - 단가는 상품이 소유하므로 상품에서 읽는다
        BigDecimal totalPrice = calculateTotalPrice(request.items(), products);

        // Order 저장
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(expireTime);
        Order order = Order.create(request.memberId(), totalPrice, expireAt);
        orderRepository.save(order);

        // 재고 예약
        for(CreateOrderRequest.OrderItem item : request.items()) {
            stockReservationService.reserveStock(order, stocks.get(item.productId()), item.quantity(), expireAt);
        }

        return order.getId();
    }

    private BigDecimal calculateTotalPrice(List<CreateOrderRequest.OrderItem> items, Map<Long, Product> products) {
        return items.stream()
            .map(item -> products.get(item.productId()).getPrice()
                .multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
