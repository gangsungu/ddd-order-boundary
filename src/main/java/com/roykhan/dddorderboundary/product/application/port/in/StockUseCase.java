package com.roykhan.dddorderboundary.product.application.port.in;


// 입력 포트 - 재고 조회와, 주문 ID 단위의 재고 예약·확정·해제·만료
public interface StockUseCase {

    StockInfo findByProductId(Long productId);

    void reserve(ReserveStockCommand command);

    // 결제 성공 - 예약을 확정해 총 재고를 차감한다
    void confirmReservations(Long orderId);

    // 주문 취소·결제 실패 - 예약을 해제해 가용 수량을 돌려준다
    void cancelReservations(Long orderId);

    // 결제 마감 경과 - 예약을 만료시켜 가용 수량을 돌려준다
    void expireReservations(Long orderId);
}
