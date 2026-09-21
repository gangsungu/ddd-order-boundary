package com.roykhan.dddorderboundary.payment.adapter.in.web;

import com.roykhan.dddorderboundary.common.response.ApiResponse;
import com.roykhan.dddorderboundary.payment.application.port.in.PaymentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentUseCase paymentUseCase;

    // 목업 결제 페이지(/payment.html)가 PG 콜백 대신 호출한다
    @PostMapping("/payment/{orderId}/result")
    @Operation(summary = "목업 결제 결과 반영", description = "성공이면 예약을 확정해 주문을 확정하고, 실패면 예약을 해제해 재고를 복원")
    public ApiResponse<Void> applyResult(@PathVariable long orderId, @Valid @RequestBody PaymentResultRequest request) {
        paymentUseCase.applyResult(orderId, request.result());
        return ApiResponse.success("결제 결과를 반영하였습니다.");
    }
}
