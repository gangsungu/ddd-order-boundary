package com.roykhan.dddorderboundary.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.roykhan.dddorderboundary.common.exception.BusinessException;
import com.roykhan.dddorderboundary.common.exception.StockErrorCode;
import com.roykhan.dddorderboundary.domain.stock.Stock;
import com.roykhan.dddorderboundary.domain.stock.dto.StockInfo;
import com.roykhan.dddorderboundary.domain.stock.enums.StockStatus;
import com.roykhan.dddorderboundary.domain.stock.repository.StockRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService 단위 테스트")
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    @DisplayName("예약이 걸린 재고는 총 재고와 가용 수량의 차이를 예약 수량으로 보여준다")
    void 예약_중인_재고_조회() {
        Stock stock = Stock.create(1L, 10);
        stock.reserve(3);
        given(stockRepository.findByProductId(1L)).willReturn(Optional.of(stock));

        StockInfo info = stockService.findByProductId(1L);

        assertThat(info.productId()).isEqualTo(1L);
        assertThat(info.quantity()).isEqualTo(10);
        assertThat(info.availableQuantity()).isEqualTo(7);
        assertThat(info.reservedQuantity()).isEqualTo(3);
        assertThat(info.stockStatus()).isEqualTo(StockStatus.IN_STOCK);
    }

    @Test
    @DisplayName("예약이 확정되면 총 재고가 줄고 예약 수량은 0 이 된다")
    void 확정된_재고_조회() {
        Stock stock = Stock.create(1L, 10);
        stock.reserve(3);
        stock.confirm(3);
        given(stockRepository.findByProductId(1L)).willReturn(Optional.of(stock));

        StockInfo info = stockService.findByProductId(1L);

        assertThat(info.quantity()).isEqualTo(7);
        assertThat(info.availableQuantity()).isEqualTo(7);
        assertThat(info.reservedQuantity()).isZero();
    }

    @Test
    @DisplayName("가용 수량이 모두 예약되면 SOLD_OUT 으로 보여준다")
    void 품절() {
        Stock stock = Stock.create(1L, 2);
        stock.reserve(2);
        given(stockRepository.findByProductId(1L)).willReturn(Optional.of(stock));

        StockInfo info = stockService.findByProductId(1L);

        assertThat(info.availableQuantity()).isZero();
        assertThat(info.reservedQuantity()).isEqualTo(2);
        assertThat(info.stockStatus()).isEqualTo(StockStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("재고가 없으면 STOCK_NOT_FOUND 로 실패한다")
    void 재고_없음() {
        given(stockRepository.findByProductId(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.findByProductId(99L))
            .isInstanceOf(BusinessException.class)
            .hasMessage(StockErrorCode.STOCK_NOT_FOUND.getMessage())
            .extracting("errorCode")
            .isEqualTo(StockErrorCode.STOCK_NOT_FOUND);
    }
}
