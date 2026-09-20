package com.roykhan.dddorderboundary.domain.order.controller;

import com.roykhan.dddorderboundary.common.response.ApiResponse;
import com.roykhan.dddorderboundary.domain.order.dto.CreateOrderRequest;
import com.roykhan.dddorderboundary.domain.order.dto.OrderInfo;
import com.roykhan.dddorderboundary.domain.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/order")
    @Operation(summary = "주문 생성", description = "주문을 생성하고 재고를 예약")
    public ApiResponse<OrderInfo> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        long orderId = orderService.createOrder(request);
        OrderInfo orderInfo = new OrderInfo(orderId);
        return ApiResponse.success("주문 생성에 성공했습니다.", orderInfo);
    }

    @PatchMapping("/order/{orderId}/cancel")
    @Operation(summary = "생성된 주문 취소", description = "생성된 주문을 취소하고 예약된 재고를 반환")
    public ApiResponse<Void> cancelOrder(@PathVariable long orderId) {
        orderService.cancelOrder(orderId);
        return ApiResponse.success("생성된 주문을 취소하였습니다.");
    }
}
