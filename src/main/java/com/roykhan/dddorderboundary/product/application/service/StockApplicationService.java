package com.roykhan.dddorderboundary.product.application.service;

import com.roykhan.dddorderboundary.product.application.dto.ReserveStockCommand;
import com.roykhan.dddorderboundary.product.application.dto.StockInfo;
import com.roykhan.dddorderboundary.product.application.usecase.StockUseCase;
import com.roykhan.dddorderboundary.product.domain.exception.StockErrorCode;
import com.roykhan.dddorderboundary.product.domain.model.Stock;
import com.roykhan.dddorderboundary.product.domain.model.StockReservation;
import com.roykhan.dddorderboundary.product.domain.repository.StockRepository;
import com.roykhan.dddorderboundary.product.domain.repository.StockReservationRepository;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 재고 조회와, 주문 ID 단위로 재고를 예약하고 그 예약을 확정·해제·만료한다
@Service
@RequiredArgsConstructor
public class StockApplicationService implements StockUseCase {

    private final StockRepository stockRepository;
    private final StockReservationRepository stockReservationRepository;

    @Override
    @Transactional(readOnly = true)
    public StockInfo findByProductId(Long productId) {
        return StockInfo.from(getStock(productId));
    }

    @Override
    @Transactional
    public void reserve(ReserveStockCommand command) {
        for(ReserveStockCommand.Line line : command.lines()) {
            Stock stock = getStock(line.productId());
            stockReservationRepository.save(
                StockReservation.create(stock, command.orderId(), line.quantity(), command.expireAt()));
        }
    }

    @Override
    @Transactional
    public void confirmReservations(Long orderId) {
        forEachReservation(orderId, StockReservation::confirm);
    }

    @Override
    @Transactional
    public void cancelReservations(Long orderId) {
        forEachReservation(orderId, StockReservation::cancel);
    }

    @Override
    @Transactional
    public void expireReservations(Long orderId) {
        forEachReservation(orderId, StockReservation::expire);
    }

    private Stock getStock(Long productId) {
        return stockRepository.findByProductId(productId)
            .orElseThrow(StockErrorCode.STOCK_NOT_FOUND::exception);
    }

    private void forEachReservation(Long orderId, Consumer<StockReservation> action) {
        List<StockReservation> reservations = stockReservationRepository.findAllByOrderId(orderId);
        reservations.forEach(action);
    }
}
