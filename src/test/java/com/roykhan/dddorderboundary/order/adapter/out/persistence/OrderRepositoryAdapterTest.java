package com.roykhan.dddorderboundary.order.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderRepositoryAdapter 단위 테스트")
class OrderRepositoryAdapterTest {

    @Mock
    private OrderJpaRepository orderJpaRepository;

    @InjectMocks
    private OrderRepositoryAdapter orderRepositoryAdapter;

    @Test
    @DisplayName("포트가 받은 건수를 첫 페이지 요청으로 바꿔 Spring Data 에 넘긴다")
    void 만료_대상_건수_변환() {
        LocalDateTime now = LocalDateTime.now();
        given(orderJpaRepository.findIdsByStatusAndExpireAtBefore(OrderStatus.PENDING, now, PageRequest.of(0, 50)))
            .willReturn(List.of(1L, 2L));

        List<Long> orderIds = orderRepositoryAdapter.findIdsByStatusAndExpireAtBefore(OrderStatus.PENDING, now, 50);

        assertThat(orderIds).containsExactly(1L, 2L);
    }
}
