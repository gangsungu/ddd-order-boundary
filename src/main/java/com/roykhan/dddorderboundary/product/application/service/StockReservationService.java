package com.roykhan.dddorderboundary.product.application.service;

import com.roykhan.dddorderboundary.product.domain.exception.StockErrorCode;
import com.roykhan.dddorderboundary.product.domain.model.Stock;
import com.roykhan.dddorderboundary.product.domain.model.StockReservation;
import com.roykhan.dddorderboundary.product.domain.repository.StockRepository;
import com.roykhan.dddorderboundary.product.domain.repository.StockReservationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 주문 ID 단위로 재고를 예약하고, 그 예약을 확정·해제·만료한다
@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final StockRepository stockRepository;
    private final StockReservationRepository stockReservationRepository;

    @Transactional
    public void reserve(Long orderId, Long productId, int quantity, LocalDateTime expireAt) {
        Stock stock = stockRepository.findByProductId(productId)
            .orElseThrow(StockErrorCode.STOCK_NOT_FOUND::exception);

        stockReservationRepository.save(StockReservation.create(stock, orderId, quantity, expireAt));
    }

    // 결제 성공 - 예약을 확정해 총 재고를 차감한다
    @Transactional
    public void confirm(Long orderId) {
        forEachReservation(orderId, StockReservation::confirm);
    }

    // 주문 취소·결제 실패 - 예약을 해제해 가용 수량을 돌려준다
    @Transactional
    public void cancel(Long orderId) {
        forEachReservation(orderId, StockReservation::cancel);
    }

    // 결제 마감 경과 - 예약을 만료시켜 가용 수량을 돌려준다
    @Transactional
    public void expire(Long orderId) {
        forEachReservation(orderId, StockReservation::expire);
    }

    private void forEachReservation(Long orderId, Consumer<StockReservation> action) {
        List<StockReservation> reservations = stockReservationRepository.findAllByOrderId(orderId);
        reservations.forEach(action);
    }
}
