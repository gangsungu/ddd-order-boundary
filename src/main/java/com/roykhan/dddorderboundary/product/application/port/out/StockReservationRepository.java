package com.roykhan.dddorderboundary.product.application.port.out;

import com.roykhan.dddorderboundary.product.domain.model.StockReservation;
import java.util.List;

// 출력 포트 - 재고 예약 저장소. 예약은 주문 ID 단위로 확정·해제되므로 주문 ID 로 찾는다
public interface StockReservationRepository {

    StockReservation save(StockReservation reservation);

    List<StockReservation> findAllByOrderId(Long orderId);
}
