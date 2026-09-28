package com.roykhan.dddorderboundary.order.adapter.out.product;

import com.roykhan.dddorderboundary.order.adapter.out.product.acl.StockErrorTranslator;
import com.roykhan.dddorderboundary.order.application.port.out.StockLine;
import com.roykhan.dddorderboundary.order.application.port.out.StockPort;
import com.roykhan.dddorderboundary.product.application.port.in.ReserveStockCommand;
import com.roykhan.dddorderboundary.product.application.port.in.StockUseCase;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 출력 어댑터 - 주문의 재고 포트를 상품 컨텍스트의 재고 유스케이스 호출로 구현한다.
// 같은 JVM 안의 호출이라 재고 변경이 주문 트랜잭션에 함께 묶인다.
// 오가는 것을 양쪽 모두 번역한다. 요청은 주문의 표현을 상품의 커맨드로, 예외는 상품의 코드를 주문의 코드로 (ACL)
@Component
@RequiredArgsConstructor
public class StockAdapter implements StockPort {

    private final StockUseCase stockUseCase;
    private final StockErrorTranslator errorTranslator;

    @Override
    public void reserve(Long orderId, List<StockLine> lines, LocalDateTime expireAt) {
        List<ReserveStockCommand.Line> reserveLines = lines.stream()
            .map(line -> new ReserveStockCommand.Line(line.productId(), line.quantity()))
            .toList();
        errorTranslator.run(() -> stockUseCase.reserve(new ReserveStockCommand(orderId, reserveLines, expireAt)));
    }

    @Override
    public void confirm(Long orderId) {
        errorTranslator.run(() -> stockUseCase.confirmReservations(orderId));
    }

    @Override
    public void cancel(Long orderId) {
        errorTranslator.run(() -> stockUseCase.cancelReservations(orderId));
    }

    @Override
    public void expire(Long orderId) {
        errorTranslator.run(() -> stockUseCase.expireReservations(orderId));
    }
}
