package com.roykhan.dddorderboundary.payment.application.usecase;

import com.roykhan.dddorderboundary.payment.domain.model.PaymentResult;

// 입력 포트 - 결제 결과를 받아 반영한다. 지금은 목업 결제 페이지가 PG 콜백 대신 부른다
public interface PaymentUseCase {

    void applyResult(long orderId, PaymentResult result);
}
