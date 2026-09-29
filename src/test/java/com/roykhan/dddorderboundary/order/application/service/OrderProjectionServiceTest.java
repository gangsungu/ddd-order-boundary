package com.roykhan.dddorderboundary.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.roykhan.dddorderboundary.order.application.port.in.OrderInfo;
import com.roykhan.dddorderboundary.order.application.port.out.OrderViewRepository;
import com.roykhan.dddorderboundary.order.domain.event.OrderCancelled;
import com.roykhan.dddorderboundary.order.domain.event.OrderConfirmed;
import com.roykhan.dddorderboundary.order.domain.event.OrderExpired;
import com.roykhan.dddorderboundary.order.domain.event.OrderPaymentFailed;
import com.roykhan.dddorderboundary.order.domain.event.OrderPlaced;
import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 이벤트만 보고 읽기 모델을 어떻게 그리는지 본다. 쓰기 모델(Order)은 등장하지 않는다
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderProjectionService 단위 테스트")
class OrderProjectionServiceTest {

    private static final LocalDateTime EXPIRE_AT = LocalDateTime.of(2026, 9, 30, 20, 0);
    private static final LocalDateTime AT = LocalDateTime.of(2026, 9, 30, 19, 55);

    @Mock
    private OrderViewRepository orderViewRepository;

    @InjectMocks
    private OrderProjectionService projectionService;

    @Captor
    private ArgumentCaptor<OrderInfo> viewCaptor;

    private static OrderInfo pendingView() {
        return new OrderInfo(1L, 7L, OrderStatus.PENDING, new BigDecimal("178000.00"), EXPIRE_AT, null, null,
            List.of(new OrderInfo.Item(1L, "글렌피딕 12년", new BigDecimal("89000.00"), 2, new BigDecimal("178000.00"))));
    }

    @Test
    @DisplayName("OrderPlaced 로 PENDING 읽기 모델을 만들고 항목 금액을 계산해 둔다")
    void 주문_생성() {
        projectionService.project(new OrderPlaced(1L, 7L, new BigDecimal("178000.00"), EXPIRE_AT,
            List.of(new OrderPlaced.Item(1L, "글렌피딕 12년", new BigDecimal("89000.00"), 2)), AT));

        verify(orderViewRepository).save(viewCaptor.capture());
        OrderInfo view = viewCaptor.getValue();
        assertThat(view.id()).isEqualTo(1L);
        assertThat(view.memberId()).isEqualTo(7L);
        assertThat(view.orderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(view.totalPrice()).isEqualByComparingTo("178000.00");
        assertThat(view.expireAt()).isEqualTo(EXPIRE_AT);
        assertThat(view.confirmedAt()).isNull();
        assertThat(view.cancelledAt()).isNull();
        assertThat(view.items()).singleElement().satisfies(item -> {
            assertThat(item.productName()).isEqualTo("글렌피딕 12년");
            assertThat(item.amount()).isEqualByComparingTo("178000.00");
        });
    }

    @Test
    @DisplayName("OrderConfirmed 로 상태와 확정 시각만 바꾸고 나머지는 그대로 둔다")
    void 주문_확정() {
        given(orderViewRepository.findById(1L)).willReturn(Optional.of(pendingView()));

        projectionService.project(new OrderConfirmed(1L, AT));

        verify(orderViewRepository).save(viewCaptor.capture());
        OrderInfo view = viewCaptor.getValue();
        assertThat(view.orderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(view.confirmedAt()).isEqualTo(AT);
        assertThat(view.cancelledAt()).isNull();
        assertThat(view.items()).isEqualTo(pendingView().items());
    }

    @Test
    @DisplayName("취소·결제 실패·만료는 각자의 상태로 바꾸고 풀린 시각을 cancelledAt 에 남긴다")
    void 예약_해제() {
        given(orderViewRepository.findById(1L)).willReturn(Optional.of(pendingView()));

        projectionService.project(new OrderCancelled(1L, AT));
        projectionService.project(new OrderPaymentFailed(1L, AT));
        projectionService.project(new OrderExpired(1L, AT));

        verify(orderViewRepository, times(3)).save(viewCaptor.capture());
        assertThat(viewCaptor.getAllValues())
            .extracting(OrderInfo::orderStatus)
            .containsExactly(OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED, OrderStatus.EXPIRED);
        assertThat(viewCaptor.getAllValues()).allSatisfy(view -> {
            assertThat(view.cancelledAt()).isEqualTo(AT);
            assertThat(view.confirmedAt()).isNull();
        });
    }

    @Test
    @DisplayName("읽기 모델에 없는 주문의 이벤트는 새로 만들지 않고 건너뛴다")
    void 읽기_모델_없음() {
        given(orderViewRepository.findById(1L)).willReturn(Optional.empty());

        projectionService.project(new OrderConfirmed(1L, AT));

        verify(orderViewRepository, never()).save(any());
    }
}
