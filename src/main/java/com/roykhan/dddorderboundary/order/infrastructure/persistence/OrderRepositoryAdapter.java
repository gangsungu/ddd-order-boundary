package com.roykhan.dddorderboundary.order.infrastructure.persistence;

import com.roykhan.dddorderboundary.order.domain.model.Order;
import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import com.roykhan.dddorderboundary.order.domain.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

// 출력 어댑터 - 주문 저장소 포트를 Spring Data JPA 로 구현한다.
// 포트는 건수(limit)만 받고, Spring Data 의 페이지 요청으로 바꾸는 일은 여기서 한다
@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findById(id);
    }

    @Override
    public List<Long> findIdsByStatusAndExpireAtBefore(OrderStatus status, LocalDateTime now, int limit) {
        return orderJpaRepository.findIdsByStatusAndExpireAtBefore(status, now, PageRequest.of(0, limit));
    }
}
