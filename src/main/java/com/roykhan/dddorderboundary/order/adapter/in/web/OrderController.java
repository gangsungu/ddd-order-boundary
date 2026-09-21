package com.roykhan.dddorderboundary.order.adapter.in.web;

import com.roykhan.dddorderboundary.common.response.ApiResponse;
import com.roykhan.dddorderboundary.order.application.port.in.OrderInfo;
import com.roykhan.dddorderboundary.order.application.port.in.OrderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final OrderUseCase orderUseCase;

    @PostMapping("/order")
    @Operation(summary = "주문 생성", description = "주문을 생성하고 재고를 예약")
    public ApiResponse<OrderCreateInfo> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        long orderId = orderUseCase.createOrder(request.toCommand());
        OrderCreateInfo orderCreateInfo = new OrderCreateInfo(orderId);
        return ApiResponse.success("주문 생성에 성공했습니다.", orderCreateInfo);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "주문 조회", description = "주문 상태와 주문 시점의 항목 내역을 조회")
    public ApiResponse<OrderInfo> getOrder(@PathVariable long orderId) {
        OrderInfo orderInfo = orderUseCase.findById(orderId);
        return ApiResponse.success("주문을 조회하였습니다.", orderInfo);
    }

    @PatchMapping("/order/{orderId}/cancel")
    @Operation(summary = "생성된 주문 취소", description = "생성된 주문을 취소하고 예약된 재고를 반환")
    public ApiResponse<Void> cancelOrder(@PathVariable long orderId) {
        orderUseCase.cancelOrder(orderId);
        return ApiResponse.success("생성된 주문을 취소하였습니다.");
    }
}
