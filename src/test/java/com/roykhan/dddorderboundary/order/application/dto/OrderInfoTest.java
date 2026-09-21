package com.roykhan.dddorderboundary.order.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.roykhan.dddorderboundary.order.domain.model.Order;
import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("OrderInfo 변환")
class OrderInfoTest {

    private static Order order(Long id) {
        Order order = Order.create(7L, LocalDateTime.of(2026, 9, 22, 20, 0));
        order.addItem(1L, "글렌피딕 12년", new BigDecimal("89000.00"), 2);
        order.addItem(2L, "맥캘란 12년", new BigDecimal("150000.00"), 1);
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    @Test
    @DisplayName("주문의 상태와 금액을 그대로 옮긴다")
    void from_주문을_DTO로_변환한다() {
        OrderInfo info = OrderInfo.from(order(42L));

        assertThat(info.id()).isEqualTo(42L);
        assertThat(info.memberId()).isEqualTo(7L);
        assertThat(info.orderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(info.totalPrice()).isEqualByComparingTo("328000.00");
        assertThat(info.expireAt()).isEqualTo(LocalDateTime.of(2026, 9, 22, 20, 0));
        assertThat(info.confirmedAt()).isNull();
        assertThat(info.cancelledAt()).isNull();
    }

    @Test
    @DisplayName("주문 시점에 복사해 둔 상품명과 단가를 항목에 담고 금액을 계산한다")
    void from_항목을_변환한다() {
        OrderInfo info = OrderInfo.from(order(42L));

        assertThat(info.items()).hasSize(2);

        OrderInfo.Item first = info.items().getFirst();
        assertThat(first.productId()).isEqualTo(1L);
        assertThat(first.productName()).isEqualTo("글렌피딕 12년");
        assertThat(first.unitPrice()).isEqualByComparingTo("89000.00");
        assertThat(first.quantity()).isEqualTo(2);
        assertThat(first.amount()).isEqualByComparingTo("178000.00");

        assertThat(info.items().getLast().amount()).isEqualByComparingTo("150000.00");
    }

    @Test
    @DisplayName("변환한 항목 목록으로는 원본 주문을 바꿀 수 없다")
    void from_항목_목록은_읽기_전용이다() {
        OrderInfo info = OrderInfo.from(order(42L));

        assertThatThrownBy(() -> info.items().add(null))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("아직 저장되지 않은 주문은 id가 null인 채로 변환된다")
    void from_영속화_전이면_id가_null이다() {
        OrderInfo info = OrderInfo.from(order(null));

        assertThat(info.id()).isNull();
        assertThat(info.items()).hasSize(2);
    }
}
