package com.roykhan.dddorderboundary.order.adapter.out.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.roykhan.dddorderboundary.order.application.port.out.StockLine;
import com.roykhan.dddorderboundary.product.application.port.in.ReserveStockCommand;
import com.roykhan.dddorderboundary.product.application.port.in.StockUseCase;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockAdapter 단위 테스트")
class StockAdapterTest {

    @Mock
    private StockUseCase stockUseCase;

    @InjectMocks
    private StockAdapter stockAdapter;

    @Captor
    private ArgumentCaptor<ReserveStockCommand> commandCaptor;

    @Test
    @DisplayName("예약 요청을 재고 컨텍스트의 예약 커맨드 하나로 옮긴다")
    void 예약_커맨드로_변환() {
        LocalDateTime expireAt = LocalDateTime.of(2026, 9, 22, 20, 0);

        stockAdapter.reserve(42L, List.of(new StockLine(1L, 2), new StockLine(2L, 3)), expireAt);

        verify(stockUseCase).reserve(commandCaptor.capture());
        ReserveStockCommand command = commandCaptor.getValue();
        assertThat(command.orderId()).isEqualTo(42L);
        assertThat(command.expireAt()).isEqualTo(expireAt);
        assertThat(command.lines()).containsExactly(
            new ReserveStockCommand.Line(1L, 2),
            new ReserveStockCommand.Line(2L, 3));
    }

    @Test
    @DisplayName("확정·해제·만료 요청을 주문 ID 그대로 재고 유스케이스의 예약 확정·해제·만료로 넘긴다")
    void 확정_해제_만료_위임() {
        stockAdapter.confirm(1L);
        stockAdapter.cancel(2L);
        stockAdapter.expire(3L);

        verify(stockUseCase).confirmReservations(1L);
        verify(stockUseCase).cancelReservations(2L);
        verify(stockUseCase).expireReservations(3L);
    }
}
