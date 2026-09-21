package com.roykhan.dddorderboundary.payment.adapter.in.web;

import com.roykhan.dddorderboundary.payment.domain.model.PaymentResult;
import jakarta.validation.constraints.NotNull;

public record PaymentResultRequest(
    @NotNull(message = "결제 결과는 필수입니다.")
    PaymentResult result
) {}
