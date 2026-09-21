package com.roykhan.dddorderboundary.order.infrastructure.product;

import com.roykhan.dddorderboundary.order.application.port.StockLine;
import com.roykhan.dddorderboundary.order.application.port.StockPort;
import com.roykhan.dddorderboundary.product.application.dto.ReserveStockCommand;
import com.roykhan.dddorderboundary.product.application.usecase.StockUseCase;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 출력 어댑터 - 주문의 재고 포트를 상품 컨텍스트의 재고 유스케이스 호출로 구현한다.
// 같은 JVM 안의 호출이라 재고 변경이 주문 트랜잭션에 함께 묶인다.
// 섹션 3 에서 네트워크 호출로 바뀌면 이 보장이 사라지므로 이 어댑터만 교체하고 보상 흐름을 따로 세운다
@Component
@RequiredArgsConstructor
public class StockAdapter implements StockPort {

    private final StockUseCase stockUseCase;

    @Override
    public void reserve(Long orderId, List<StockLine> lines, LocalDateTime expireAt) {
        List<ReserveStockCommand.Line> reserveLines = lines.stream()
            .map(line -> new ReserveStockCommand.Line(line.productId(), line.quantity()))
            .toList();
        stockUseCase.reserve(new ReserveStockCommand(orderId, reserveLines, expireAt));
    }

    @Override
    public void confirm(Long orderId) {
        stockUseCase.confirmReservations(orderId);
    }

    @Override
    public void cancel(Long orderId) {
        stockUseCase.cancelReservations(orderId);
    }

    @Override
    public void expire(Long orderId) {
        stockUseCase.expireReservations(orderId);
    }
}
