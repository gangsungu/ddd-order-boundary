package com.roykhan.dddorderboundary.payment.domain.event;

import java.time.LocalDateTime;

// 결제가 발행하는 이벤트의 계약 - 결제 컨텍스트가 소유한다.
// 결제는 "결제가 성공했다/실패했다"는 사실만 말하고, 그 결과로 무엇을 할지는 받는 쪽이 정한다.
// 받는 쪽은 이 계약을 자기 언어로 번역해서 받는다 (ACL)
public sealed interface PaymentEvent permits PaymentSucceeded, PaymentFailed {

    long orderId();

    LocalDateTime occurredAt();
}
