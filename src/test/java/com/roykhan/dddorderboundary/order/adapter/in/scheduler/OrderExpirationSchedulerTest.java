package com.roykhan.dddorderboundary.order.adapter.in.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.roykhan.dddorderboundary.order.application.port.in.OrderUseCase;
import com.roykhan.dddorderboundary.order.domain.exception.OrderErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderExpirationScheduler 단위 테스트")
class OrderExpirationSchedulerTest {

    @Mock
    private OrderUseCase orderUseCase;

    @InjectMocks
    private OrderExpirationScheduler scheduler;

    @Test
    @DisplayName("결제 마감이 지난 주문을 한 건씩 만료시킨다")
    void 만료_대상_처리() {
        given(orderUseCase.findExpiredOrderIds(any(LocalDateTime.class), anyInt())).willReturn(List.of(1L, 2L, 3L));

        scheduler.expireOrders();

        verify(orderUseCase).expireOrder(1L);
        verify(orderUseCase).expireOrder(2L);
        verify(orderUseCase).expireOrder(3L);
    }

    @Test
    @DisplayName("만료 대상이 없으면 아무 주문도 건드리지 않는다")
    void 만료_대상_없음() {
        given(orderUseCase.findExpiredOrderIds(any(LocalDateTime.class), anyInt())).willReturn(List.of());

        scheduler.expireOrders();

        verify(orderUseCase, never()).expireOrder(anyLong());
    }

    @Test
    @DisplayName("그 사이 결제·취소가 끝나 만료할 수 없는 주문은 건너뛰고 나머지를 계속 처리한다")
    void 상태가_바뀐_주문_건너뜀() {
        given(orderUseCase.findExpiredOrderIds(any(LocalDateTime.class), anyInt())).willReturn(List.of(1L, 2L, 3L));
        doThrow(OrderErrorCode.ORDER_ALREADY_CONFIRMED.exception()).when(orderUseCase).expireOrder(2L);

        scheduler.expireOrders();

        verify(orderUseCase).expireOrder(1L);
        verify(orderUseCase).expireOrder(3L);
    }

    @Test
    @DisplayName("재고 동시 변경으로 충돌한 주문은 건너뛰고 나머지를 계속 처리한다")
    void 낙관적_락_충돌_건너뜀() {
        given(orderUseCase.findExpiredOrderIds(any(LocalDateTime.class), anyInt())).willReturn(List.of(1L, 2L));
        doThrow(new ObjectOptimisticLockingFailureException("Stock", 10L)).when(orderUseCase).expireOrder(1L);

        scheduler.expireOrders();

        verify(orderUseCase).expireOrder(2L);
    }

    @Test
    @DisplayName("예상하지 못한 오류가 나도 나머지 주문은 계속 처리한다")
    void 예상하지_못한_오류() {
        given(orderUseCase.findExpiredOrderIds(any(LocalDateTime.class), anyInt())).willReturn(List.of(1L, 2L));
        doThrow(new IllegalStateException("boom")).when(orderUseCase).expireOrder(1L);

        scheduler.expireOrders();

        verify(orderUseCase).expireOrder(2L);
    }
}
