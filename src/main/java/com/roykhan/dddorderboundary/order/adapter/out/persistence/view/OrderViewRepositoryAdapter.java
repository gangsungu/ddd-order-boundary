package com.roykhan.dddorderboundary.order.adapter.out.persistence.view;

import com.roykhan.dddorderboundary.order.application.port.in.OrderInfo;
import com.roykhan.dddorderboundary.order.application.port.out.OrderViewRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

// 출력 어댑터 - 주문 읽기 모델 저장소 포트를 Spring Data JPA 로 구현한다.
// 식별자를 직접 넣으므로 save 는 merge 가 되어, 같은 주문 ID 면 통째로 덮어쓴다
@Repository
@RequiredArgsConstructor
public class OrderViewRepositoryAdapter implements OrderViewRepository {

    private final OrderViewJpaRepository orderViewJpaRepository;

    @Override
    public void save(OrderInfo view) {
        orderViewJpaRepository.save(OrderViewEntity.from(view));
    }

    @Override
    public Optional<OrderInfo> findById(long orderId) {
        return orderViewJpaRepository.findById(orderId).map(OrderViewEntity::toInfo);
    }
}
