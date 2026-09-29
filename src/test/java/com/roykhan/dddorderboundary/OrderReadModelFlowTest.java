package com.roykhan.dddorderboundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.roykhan.dddorderboundary.order.application.port.in.CreateOrderCommand;
import com.roykhan.dddorderboundary.order.application.port.in.OrderCommandUseCase;
import com.roykhan.dddorderboundary.order.application.port.in.OrderInfo;
import com.roykhan.dddorderboundary.order.application.port.in.OrderQueryUseCase;
import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import com.roykhan.dddorderboundary.product.application.port.in.ProductUseCase;
import com.roykhan.dddorderboundary.product.application.port.in.RegisterProductCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

// 쓰기 → 주문 이벤트 → 읽기 모델 갱신이 실제 트랜잭션에서 이어지는지 확인한다.
// 리스너는 쓰기 트랜잭션이 커밋된 뒤에 돌기 때문에 테스트에 @Transactional 을 걸지 않는다
@SpringBootTest
@DisplayName("주문 읽기 모델 통합 테스트")
class OrderReadModelFlowTest {

    @Autowired
    private OrderCommandUseCase orderCommandUseCase;

    @Autowired
    private OrderQueryUseCase orderQueryUseCase;

    @Autowired
    private ProductUseCase productUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long productId;

    @BeforeEach
    void setUp() {
        // 상품명은 중복 등록을 막으므로 테스트마다 다른 이름을 쓴다
        productId = productUseCase.register(
            new RegisterProductCommand("상품-" + UUID.randomUUID(), "설명", new BigDecimal("1000.00"), 10));
    }

    private long placeOrder(int quantity) {
        return orderCommandUseCase.createOrder(
            new CreateOrderCommand(7L, List.of(new CreateOrderCommand.Line(productId, quantity))));
    }

    @Test
    @DisplayName("주문을 만들면 읽기 모델에 주문 시점의 항목과 금액이 담긴다")
    void 주문_생성() {
        long orderId = placeOrder(3);

        OrderInfo view = orderQueryUseCase.findById(orderId);
        assertThat(view.memberId()).isEqualTo(7L);
        assertThat(view.orderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(view.totalPrice()).isEqualByComparingTo("3000.00");
        assertThat(view.items()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(productId);
            assertThat(item.quantity()).isEqualTo(3);
            assertThat(item.amount()).isEqualByComparingTo("3000.00");
        });
    }

    @Test
    @DisplayName("주문을 취소하면 읽기 모델의 상태와 취소 시각이 바뀐다")
    void 주문_취소() {
        long orderId = placeOrder(1);

        orderCommandUseCase.cancelOrder(orderId);

        OrderInfo view = orderQueryUseCase.findById(orderId);
        assertThat(view.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(view.cancelledAt()).isNotNull();
        assertThat(view.items()).hasSize(1);
    }

    @Test
    @DisplayName("재고가 부족해 되돌아간 주문은 읽기 모델에 나타나지 않는다")
    void 롤백된_주문() {
        Integer before = countViews();

        assertThatThrownBy(() -> placeOrder(11));

        assertThat(countViews()).isEqualTo(before);
    }

    @Test
    @DisplayName("조회는 쓰기 모델이 아닌 읽기 모델에서 한다 — 읽기 모델이 없으면 주문이 있어도 찾지 못한다")
    void 읽기_모델에서_조회() {
        long orderId = placeOrder(1);
        jdbcTemplate.update("delete from orders.order_view_items where order_id = ?", orderId);
        jdbcTemplate.update("delete from orders.order_view where order_id = ?", orderId);

        assertThatThrownBy(() -> orderQueryUseCase.findById(orderId))
            .hasMessage("주문을 찾을 수 없습니다.");
    }

    private Integer countViews() {
        return jdbcTemplate.queryForObject("select count(*) from orders.order_view", Integer.class);
    }
}
