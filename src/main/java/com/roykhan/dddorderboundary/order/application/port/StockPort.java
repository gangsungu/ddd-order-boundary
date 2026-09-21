package com.roykhan.dddorderboundary.order.application.port;

import java.time.LocalDateTime;
import java.util.List;

// 출력 포트 - 주문이 재고 컨텍스트에 요청하는 것. 예약은 주문 ID 로 묶이고, 확정·해제도 주문 ID 로 한다
public interface StockPort {

    // 주문의 항목들을 같은 결제 마감으로 예약한다
    void reserve(Long orderId, List<StockLine> lines, LocalDateTime expireAt);

    // 결제 성공 - 예약을 확정한다
    void confirm(Long orderId);

    // 사용자 취소·결제 실패 - 예약을 해제한다
    void cancel(Long orderId);

    // 결제 마감 경과 - 예약을 만료시킨다
    void expire(Long orderId);
}
