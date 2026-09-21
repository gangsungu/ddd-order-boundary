package com.roykhan.dddorderboundary.domain.payment.dto;

import com.roykhan.dddorderboundary.domain.payment.enums.PaymentResult;
import jakarta.validation.constraints.NotNull;

public record PaymentResultRequest(
    @NotNull(message = "결제 결과는 필수입니다.")
    PaymentResult result
) {}
