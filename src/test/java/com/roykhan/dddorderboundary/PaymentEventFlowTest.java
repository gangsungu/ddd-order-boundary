package com.roykhan.dddorderboundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.roykhan.dddorderboundary.order.application.port.in.CreateOrderCommand;
import com.roykhan.dddorderboundary.order.application.port.in.OrderUseCase;
import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import com.roykhan.dddorderboundary.payment.application.port.in.PaymentUseCase;
import com.roykhan.dddorderboundary.payment.domain.model.PaymentResult;
import com.roykhan.dddorderboundary.product.application.port.in.ProductUseCase;
import com.roykhan.dddorderboundary.product.application.port.in.RegisterProductCommand;
import com.roykhan.dddorderboundary.product.application.port.in.StockInfo;
import com.roykhan.dddorderboundary.product.application.port.in.StockUseCase;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 결제 → 주문이 호출 없이 이벤트로만 이어지는지 실제 트랜잭션으로 확인한다.
// 리스너는 결제 트랜잭션이 커밋된 뒤에 돌기 때문에 테스트에 @Transactional 을 걸지 않는다
@SpringBootTest
@DisplayName("결제 이벤트 흐름 통합 테스트")
class PaymentEventFlowTest {

    @Autowired
    private PaymentUseCase paymentUseCase;

    @Autowired
    private OrderUseCase orderUseCase;

    @Autowired
    private ProductUseCase productUseCase;

    @Autowired
    private StockUseCase stockUseCase;

    private Long productId;
    private long orderId;

    @BeforeEach
    void setUp() {
        // 상품명은 중복 등록을 막으므로 테스트마다 다른 이름을 쓴다
        productId = productUseCase.register(new RegisterProductCommand("상품-" + UUID.randomUUID(), "설명", new BigDecimal("1000"), 10));
        orderId = orderUseCase.createOrder(new CreateOrderCommand(1L, List.of(new CreateOrderCommand.Line(productId, 3))));
    }

    @Test
    @DisplayName("결제 성공 이벤트로 주문이 확정되고 예약이 재고에서 차감된다")
    void 결제_성공() {
        paymentUseCase.applyResult(orderId, PaymentResult.SUCCESS);

        assertThat(orderUseCase.findById(orderId).orderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        StockInfo stock = stockUseCase.findByProductId(productId);
        assertThat(stock.quantity()).isEqualTo(7);
        assertThat(stock.reservedQuantity()).isZero();
    }

    @Test
    @DisplayName("결제 실패 이벤트로 주문이 결제 실패가 되고 예약한 재고가 돌아온다")
    void 결제_실패() {
        paymentUseCase.applyResult(orderId, PaymentResult.FAILURE);

        assertThat(orderUseCase.findById(orderId).orderStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        StockInfo stock = stockUseCase.findByProductId(productId);
        assertThat(stock.quantity()).isEqualTo(10);
        assertThat(stock.availableQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("주문이 결과를 반영하지 못해도 결제는 주문의 에러를 받지 않는다")
    void 반영_거절() {
        orderUseCase.cancelOrder(orderId);

        assertThatCode(() -> paymentUseCase.applyResult(orderId, PaymentResult.SUCCESS)).doesNotThrowAnyException();
        assertThat(orderUseCase.findById(orderId).orderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
