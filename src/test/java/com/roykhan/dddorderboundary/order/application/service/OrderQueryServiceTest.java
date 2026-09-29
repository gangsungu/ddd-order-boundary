package com.roykhan.dddorderboundary.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.roykhan.dddorderboundary.common.exception.BusinessException;
import com.roykhan.dddorderboundary.order.application.port.in.OrderInfo;
import com.roykhan.dddorderboundary.order.application.port.out.OrderViewRepository;
import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import com.roykhan.dddorderboundary.order.exception.out.OrderLookupErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderQueryService 단위 테스트")
class OrderQueryServiceTest {

    @Mock
    private OrderViewRepository orderViewRepository;

    @InjectMocks
    private OrderQueryService queryService;

    @Test
    @DisplayName("읽기 모델을 그대로 돌려준다")
    void 조회_성공() {
        OrderInfo view = new OrderInfo(1L, 7L, OrderStatus.PENDING, new BigDecimal("1000"),
            LocalDateTime.now(), null, null, List.of());
        given(orderViewRepository.findById(1L)).willReturn(Optional.of(view));

        assertThat(queryService.findById(1L)).isSameAs(view);
    }

    @Test
    @DisplayName("읽기 모델에 없으면 ORDER_NOT_FOUND 로 실패한다")
    void 조회_실패() {
        given(orderViewRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.findById(99L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(OrderLookupErrorCode.ORDER_NOT_FOUND);
    }
}
