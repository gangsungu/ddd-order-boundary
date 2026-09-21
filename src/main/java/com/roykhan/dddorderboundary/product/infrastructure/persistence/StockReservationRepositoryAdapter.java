package com.roykhan.dddorderboundary.product.infrastructure.persistence;

import com.roykhan.dddorderboundary.product.domain.model.StockReservation;
import com.roykhan.dddorderboundary.product.domain.repository.StockReservationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

// 출력 어댑터 - 재고 예약 저장소 포트를 Spring Data JPA 로 구현한다
@Repository
@RequiredArgsConstructor
public class StockReservationRepositoryAdapter implements StockReservationRepository {

    private final StockReservationJpaRepository stockReservationJpaRepository;

    @Override
    public StockReservation save(StockReservation reservation) {
        return stockReservationJpaRepository.save(reservation);
    }

    @Override
    public List<StockReservation> findAllByOrderId(Long orderId) {
        return stockReservationJpaRepository.findAllByOrderId(orderId);
    }
}
