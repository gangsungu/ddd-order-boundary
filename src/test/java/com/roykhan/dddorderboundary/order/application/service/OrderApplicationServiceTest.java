package com.roykhan.dddorderboundary.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.roykhan.dddorderboundary.common.exception.BusinessException;
import com.roykhan.dddorderboundary.order.application.dto.CreateOrderCommand;
import com.roykhan.dddorderboundary.order.application.dto.OrderInfo;
import com.roykhan.dddorderboundary.order.domain.exception.OrderErrorCode;
import com.roykhan.dddorderboundary.order.domain.model.Order;
import com.roykhan.dddorderboundary.order.domain.model.OrderItem;
import com.roykhan.dddorderboundary.order.domain.model.OrderStatus;
import com.roykhan.dddorderboundary.order.domain.repository.OrderRepository;
import com.roykhan.dddorderboundary.product.application.dto.ProductInfo;
import com.roykhan.dddorderboundary.product.application.dto.ReserveStockCommand;
import com.roykhan.dddorderboundary.product.application.usecase.ProductUseCase;
import com.roykhan.dddorderboundary.product.application.usecase.StockUseCase;
import com.roykhan.dddorderboundary.product.domain.exception.ReservationErrorCode;
import com.roykhan.dddorderboundary.product.domain.exception.StockErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

// 재고 수량의 변화는 StockApplicationServiceTest 가 확인한다.
// 여기서는 주문 상태 전이와, 그 결과로 어떤 주문 ID 의 예약을 확정·해제하도록 요청하는지만 본다
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderApplicationService 단위 테스트")
class OrderApplicationServiceTest {

    private static final int EXPIRE_MINUTES = 10;

    @Mock
    private ProductUseCase productUseCase;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StockUseCase stockUseCase;

    @InjectMocks
    private OrderApplicationService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<ReserveStockCommand> reserveCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "expireTime", EXPIRE_MINUTES);
    }

    // 상품 컨텍스트가 유스케이스로 돌려주는 조회 결과
    private static ProductInfo product(Long id, String name, String price) {
        return new ProductInfo(id, name, "설명", new BigDecimal(price));
    }

    private static CreateOrderCommand command(Long memberId, CreateOrderCommand.Line... lines) {
        return new CreateOrderCommand(memberId, List.of(lines));
    }

    private static CreateOrderCommand.Line line(Long productId, int quantity) {
        return new CreateOrderCommand.Line(productId, quantity);
    }

    private static Order pendingOrder(long orderId) {
        return pendingOrder(orderId, LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
    }

    private static Order pendingOrder(long orderId, LocalDateTime expireAt) {
        Order order = Order.create(1L, expireAt);
        order.addItem(1L, "상품", new BigDecimal("1000"), 3);
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    private static Order orderWithStatus(long orderId, OrderStatus status) {
        Order order = pendingOrder(orderId);
        ReflectionTestUtils.setField(order, "orderStatus", status);
        return order;
    }

    // 저장 시점에 식별자가 부여되는 JPA 동작을 흉내낸다. 예약이 주문 ID 로 묶이므로 필요하다
    private void givenOrderSaveAssignsId(long id) {
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", id);
            return order;
        });
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("주문을 PENDING 으로 저장하고 부여된 주문 ID 를 반환한다")
        void 주문_생성_성공() {
            given(productUseCase.findAllByIds(List.of(1L))).willReturn(List.of(product(1L, "위스키", "1000")));
            givenOrderSaveAssignsId(42L);

            long orderId = orderService.createOrder(command(7L, line(1L, 2)));

            assertThat(orderId).isEqualTo(42L);

            verify(orderRepository).save(orderCaptor.capture());
            Order saved = orderCaptor.getValue();
            assertThat(saved.getMemberId()).isEqualTo(7L);
            assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(saved.getConfirmedAt()).isNull();
            assertThat(saved.getCancelledAt()).isNull();
        }

        @Test
        @DisplayName("주문 시점의 상품명과 단가를 항목에 복사한다")
        void 주문_시점_스냅샷() {
            given(productUseCase.findAllByIds(List.of(1L))).willReturn(List.of(product(1L, "글렌피딕 12년", "89000.00")));
            givenOrderSaveAssignsId(1L);

            orderService.createOrder(command(1L, line(1L, 3)));

            verify(orderRepository).save(orderCaptor.capture());
            Order saved = orderCaptor.getValue();
            assertThat(saved.getItems()).hasSize(1);

            OrderItem item = saved.getItems().getFirst();
            assertThat(item.getProductId()).isEqualTo(1L);
            assertThat(item.getProductName()).isEqualTo("글렌피딕 12년");
            assertThat(item.getUnitPrice()).isEqualByComparingTo("89000.00");
            assertThat(item.getQuantity()).isEqualTo(3);
            assertThat(item.getOrder()).isSameAs(saved);
        }

        @Test
        @DisplayName("총 주문 금액을 항목 금액의 합으로 계산한다")
        void 총액_계산() {
            given(productUseCase.findAllByIds(List.of(1L, 2L)))
                .willReturn(List.of(product(1L, "상품1", "1000"), product(2L, "상품2", "250.50")));
            givenOrderSaveAssignsId(1L);

            // 1000 * 2 + 250.50 * 4 = 3002.00
            orderService.createOrder(command(1L, line(1L, 2), line(2L, 4)));

            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getTotalPrice()).isEqualByComparingTo("3002.00");
        }

        @Test
        @DisplayName("저장해서 받은 주문 ID 로 모든 항목의 재고를 한 번에 예약하고 주문과 같은 만료 시각을 넘긴다")
        void 항목별_재고_예약() {
            given(productUseCase.findAllByIds(List.of(1L, 2L)))
                .willReturn(List.of(product(1L, "상품1", "1000"), product(2L, "상품2", "2000")));
            givenOrderSaveAssignsId(42L);

            orderService.createOrder(command(1L, line(1L, 2), line(2L, 3)));

            verify(stockUseCase).reserve(reserveCaptor.capture());
            ReserveStockCommand command = reserveCaptor.getValue();
            assertThat(command.orderId()).isEqualTo(42L);
            assertThat(command.lines()).containsExactly(
                new ReserveStockCommand.Line(1L, 2),
                new ReserveStockCommand.Line(2L, 3));

            verify(orderRepository).save(orderCaptor.capture());
            assertThat(command.expireAt()).isEqualTo(orderCaptor.getValue().getExpireAt());
        }

        @Test
        @DisplayName("만료 시각을 설정된 분만큼 뒤로 잡는다")
        void 만료_시각() {
            given(productUseCase.findAllByIds(List.of(1L))).willReturn(List.of(product(1L, "상품1", "1000")));
            givenOrderSaveAssignsId(1L);

            LocalDateTime before = LocalDateTime.now();
            orderService.createOrder(command(1L, line(1L, 1)));
            LocalDateTime after = LocalDateTime.now();

            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getExpireAt())
                .isBetween(before.plusMinutes(EXPIRE_MINUTES), after.plusMinutes(EXPIRE_MINUTES));
        }

        @Test
        @DisplayName("같은 상품을 여러 줄로 주문하면 항목도 줄 수만큼 담고 각각 예약한다")
        void 같은_상품_여러_줄() {
            given(productUseCase.findAllByIds(List.of(1L))).willReturn(List.of(product(1L, "상품1", "1000")));
            givenOrderSaveAssignsId(1L);

            orderService.createOrder(command(1L, line(1L, 3), line(1L, 5)));

            verify(orderRepository).save(orderCaptor.capture());
            Order saved = orderCaptor.getValue();
            assertThat(saved.getItems()).hasSize(2);
            assertThat(saved.getItems()).extracting(OrderItem::getQuantity).containsExactly(3, 5);
            assertThat(saved.getTotalPrice()).isEqualByComparingTo("8000");

            verify(stockUseCase).reserve(reserveCaptor.capture());
            assertThat(reserveCaptor.getValue().lines()).containsExactly(
                new ReserveStockCommand.Line(1L, 3),
                new ReserveStockCommand.Line(1L, 5));
        }

        @Test
        @DisplayName("항목 목록은 읽기 전용이라 밖에서 담을 수 없다")
        void 항목_목록_읽기_전용() {
            given(productUseCase.findAllByIds(List.of(1L))).willReturn(List.of(product(1L, "상품1", "1000")));
            givenOrderSaveAssignsId(1L);

            orderService.createOrder(command(1L, line(1L, 1)));

            verify(orderRepository).save(orderCaptor.capture());
            List<OrderItem> items = orderCaptor.getValue().getItems();

            assertThatThrownBy(() -> items.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("없는 상품이 섞여 있으면 주문을 저장하지 않고 INVALID_ORDER_ITEM 으로 실패한다")
        void 상품_없음() {
            given(productUseCase.findAllByIds(List.of(1L, 9999L)))
                .willReturn(List.of(product(1L, "상품1", "1000")));

            assertThatThrownBy(() -> orderService.createOrder(command(1L, line(1L, 1), line(9999L, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessage(OrderErrorCode.INVALID_ORDER_ITEM.getMessage())
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.INVALID_ORDER_ITEM);

            verify(orderRepository, never()).save(any());
            verifyNoInteractions(stockUseCase);
        }

        // 재고 레코드의 존재는 재고 컨텍스트가 예약하면서 확인한다
        @Test
        @DisplayName("재고 레코드가 없으면 예약 단계의 STOCK_NOT_FOUND 가 그대로 전파된다")
        void 재고_레코드_없음() {
            given(productUseCase.findAllByIds(List.of(1L))).willReturn(List.of(product(1L, "상품1", "1000")));
            givenOrderSaveAssignsId(1L);
            doThrow(StockErrorCode.STOCK_NOT_FOUND.exception()).when(stockUseCase).reserve(any());

            assertThatThrownBy(() -> orderService.createOrder(command(1L, line(1L, 1))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(StockErrorCode.STOCK_NOT_FOUND);
        }

        @Test
        @DisplayName("재고가 부족하면 예약 단계의 OUT_OF_STOCK 이 그대로 전파된다")
        void 재고_부족_전파() {
            given(productUseCase.findAllByIds(List.of(1L))).willReturn(List.of(product(1L, "상품1", "1000")));
            givenOrderSaveAssignsId(1L);
            doThrow(StockErrorCode.OUT_OF_STOCK.exception()).when(stockUseCase).reserve(any());

            assertThatThrownBy(() -> orderService.createOrder(command(1L, line(1L, 5))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(StockErrorCode.OUT_OF_STOCK);
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("PENDING 주문을 취소하면 CANCELLED 로 바꾸고 그 주문의 예약 해제를 요청한다")
        void 취소_성공() {
            Order order = pendingOrder(1L);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            orderService.cancelOrder(1L);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelledAt()).isNotNull();
            verify(stockUseCase).cancelReservations(1L);
        }

        @Test
        @DisplayName("주문이 없으면 ORDER_NOT_FOUND 로 실패한다")
        void 주문_없음() {
            given(orderRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(OrderErrorCode.ORDER_NOT_FOUND.getMessage())
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);

            verifyNoInteractions(stockUseCase);
        }

        @Test
        @DisplayName("이미 취소된 주문은 ORDER_ALREADY_CANCELLED 로 실패하고 예약을 다시 해제하지 않는다")
        void 이미_취소됨() {
            given(orderRepository.findById(1L)).willReturn(Optional.of(orderWithStatus(1L, OrderStatus.CANCELLED)));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(OrderErrorCode.ORDER_ALREADY_CANCELLED.getMessage())
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_CANCELLED);

            verifyNoInteractions(stockUseCase);
        }

        @Test
        @DisplayName("만료된 주문은 ORDER_ALREADY_EXPIRED 로 실패한다")
        void 이미_만료됨() {
            given(orderRepository.findById(1L)).willReturn(Optional.of(orderWithStatus(1L, OrderStatus.EXPIRED)));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(OrderErrorCode.ORDER_ALREADY_EXPIRED.getMessage())
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_EXPIRED);

            verifyNoInteractions(stockUseCase);
        }

        @Test
        @DisplayName("확정된 주문은 ORDER_ALREADY_CONFIRMED 로 실패한다 - 환불은 결제 취소가 선행되어야 한다")
        void 이미_확정됨() {
            given(orderRepository.findById(1L)).willReturn(Optional.of(orderWithStatus(1L, OrderStatus.CONFIRMED)));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(OrderErrorCode.ORDER_ALREADY_CONFIRMED.getMessage())
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_CONFIRMED);

            verifyNoInteractions(stockUseCase);
        }

        @Test
        @DisplayName("결제에 실패한 주문은 ORDER_PAYMENT_FAILED 로 실패한다 - 예약은 이미 해제되었다")
        void 결제_실패함() {
            given(orderRepository.findById(1L)).willReturn(Optional.of(orderWithStatus(1L, OrderStatus.PAYMENT_FAILED)));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_PAYMENT_FAILED);

            verifyNoInteractions(stockUseCase);
        }
    }

    @Nested
    @DisplayName("confirmOrder")
    class ConfirmOrder {

        @Test
        @DisplayName("결제에 성공하면 CONFIRMED 로 바꾸고 그 주문의 예약 확정을 요청한다")
        void 확정_성공() {
            Order order = pendingOrder(1L);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            orderService.confirmOrder(1L);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(order.getConfirmedAt()).isNotNull();
            assertThat(order.getCancelledAt()).isNull();
            verify(stockUseCase).confirmReservations(1L);
        }

        @Test
        @DisplayName("주문이 없으면 ORDER_NOT_FOUND 로 실패한다")
        void 주문_없음() {
            given(orderRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.confirmOrder(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);

            verifyNoInteractions(stockUseCase);
        }

        @Test
        @DisplayName("이미 확정된 주문은 ORDER_ALREADY_CONFIRMED 로 실패하고 예약을 다시 확정하지 않는다")
        void 이미_확정됨() {
            given(orderRepository.findById(1L)).willReturn(Optional.of(orderWithStatus(1L, OrderStatus.CONFIRMED)));

            assertThatThrownBy(() -> orderService.confirmOrder(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_CONFIRMED);

            verifyNoInteractions(stockUseCase);
        }

        @Test
        @DisplayName("결제에 실패한 주문은 ORDER_PAYMENT_FAILED 로 실패한다")
        void 결제_실패함() {
            given(orderRepository.findById(1L)).willReturn(Optional.of(orderWithStatus(1L, OrderStatus.PAYMENT_FAILED)));

            assertThatThrownBy(() -> orderService.confirmOrder(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_PAYMENT_FAILED);

            verifyNoInteractions(stockUseCase);
        }

        // 만료 스케줄러가 아직 돌지 않아 주문은 PENDING 이지만 예약의 결제 마감은 지난 경우.
        // 예외가 트랜잭션을 되돌리므로 주문의 CONFIRMED 전이도 함께 취소된다
        @Test
        @DisplayName("예약 확정이 RESERVATION_EXPIRED 로 거절되면 그대로 전파한다")
        void 결제_마감_지남() {
            given(orderRepository.findById(1L)).willReturn(Optional.of(pendingOrder(1L)));
            doThrow(ReservationErrorCode.RESERVATION_EXPIRED.exception()).when(stockUseCase).confirmReservations(1L);

            assertThatThrownBy(() -> orderService.confirmOrder(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReservationErrorCode.RESERVATION_EXPIRED);
        }
    }

    @Nested
    @DisplayName("failPayment")
    class FailPayment {

        @Test
        @DisplayName("결제에 실패하면 PAYMENT_FAILED 로 바꾸고 그 주문의 예약 해제를 요청한다")
        void 실패_반영_성공() {
            Order order = pendingOrder(1L);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            orderService.failPayment(1L);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
            assertThat(order.getCancelledAt()).isNotNull();
            assertThat(order.getConfirmedAt()).isNull();
            verify(stockUseCase).cancelReservations(1L);
        }

        @Test
        @DisplayName("주문이 없으면 ORDER_NOT_FOUND 로 실패한다")
        void 주문_없음() {
            given(orderRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.failPayment(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);

            verifyNoInteractions(stockUseCase);
        }

        @Test
        @DisplayName("이미 확정된 주문은 ORDER_ALREADY_CONFIRMED 로 실패하고 예약을 해제하지 않는다")
        void 이미_확정됨() {
            given(orderRepository.findById(1L)).willReturn(Optional.of(orderWithStatus(1L, OrderStatus.CONFIRMED)));

            assertThatThrownBy(() -> orderService.failPayment(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_CONFIRMED);

            verifyNoInteractions(stockUseCase);
        }

        @Test
        @DisplayName("결제 마감이 지난 주문이어도 아직 PENDING 이면 예약 해제를 요청한다")
        void 결제_마감_지남() {
            Order order = pendingOrder(1L, LocalDateTime.now().minusMinutes(1));
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            orderService.failPayment(1L);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
            verify(stockUseCase).cancelReservations(1L);
        }
    }

    @Nested
    @DisplayName("findExpiredOrderIds")
    class FindExpiredOrderIds {

        @Test
        @DisplayName("PENDING 주문 중 기준 시각 이전에 마감된 것을 요청한 수만큼 조회한다")
        void 만료_대상_조회() {
            LocalDateTime now = LocalDateTime.now();
            given(orderRepository.findIdsByStatusAndExpireAtBefore(OrderStatus.PENDING, now, 100))
                .willReturn(List.of(3L, 1L));

            List<Long> orderIds = orderService.findExpiredOrderIds(now, 100);

            assertThat(orderIds).containsExactly(3L, 1L);
        }
    }

    @Nested
    @DisplayName("expireOrder")
    class ExpireOrder {

        @Test
        @DisplayName("PENDING 주문을 EXPIRED 로 바꾸고 그 주문의 예약 만료를 요청한다")
        void 만료_성공() {
            Order order = pendingOrder(1L, LocalDateTime.now().minusMinutes(1));
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            orderService.expireOrder(1L);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.EXPIRED);
            assertThat(order.getCancelledAt()).isNotNull();
            verify(stockUseCase).expireReservations(1L);
        }

        @Test
        @DisplayName("만료해도 결제 마감 시각은 덮어쓰지 않는다")
        void 마감_시각_유지() {
            LocalDateTime expireAt = LocalDateTime.now().minusMinutes(1);
            Order order = pendingOrder(1L, expireAt);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            orderService.expireOrder(1L);

            assertThat(order.getExpireAt()).isEqualTo(expireAt);
        }

        @Test
        @DisplayName("주문이 없으면 ORDER_NOT_FOUND 로 실패한다")
        void 주문_없음() {
            given(orderRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.expireOrder(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);

            verifyNoInteractions(stockUseCase);
        }

        // 스케줄러가 ID 를 읽은 뒤 결제가 먼저 끝난 경우
        @Test
        @DisplayName("이미 확정된 주문은 ORDER_ALREADY_CONFIRMED 로 실패하고 예약을 건드리지 않는다")
        void 이미_확정됨() {
            given(orderRepository.findById(1L)).willReturn(Optional.of(orderWithStatus(1L, OrderStatus.CONFIRMED)));

            assertThatThrownBy(() -> orderService.expireOrder(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_CONFIRMED);

            verifyNoInteractions(stockUseCase);
        }

        @Test
        @DisplayName("이미 취소된 주문은 ORDER_ALREADY_CANCELLED 로 실패하고 예약을 건드리지 않는다")
        void 이미_취소됨() {
            given(orderRepository.findById(1L)).willReturn(Optional.of(orderWithStatus(1L, OrderStatus.CANCELLED)));

            assertThatThrownBy(() -> orderService.expireOrder(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_CANCELLED);

            verifyNoInteractions(stockUseCase);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("주문을 항목까지 담아 OrderInfo 로 반환한다")
        void 조회_성공() {
            Order order = Order.create(7L, LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
            order.addItem(1L, "글렌피딕 12년", new BigDecimal("89000.00"), 2);
            ReflectionTestUtils.setField(order, "id", 1L);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            OrderInfo info = orderService.findById(1L);

            assertThat(info.id()).isEqualTo(1L);
            assertThat(info.memberId()).isEqualTo(7L);
            assertThat(info.orderStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(info.totalPrice()).isEqualByComparingTo("178000.00");
            assertThat(info.items()).singleElement().satisfies(item -> {
                assertThat(item.productId()).isEqualTo(1L);
                assertThat(item.productName()).isEqualTo("글렌피딕 12년");
                assertThat(item.unitPrice()).isEqualByComparingTo("89000.00");
                assertThat(item.quantity()).isEqualTo(2);
                assertThat(item.amount()).isEqualByComparingTo("178000.00");
            });
        }

        @Test
        @DisplayName("주문이 없으면 ORDER_NOT_FOUND 로 실패한다")
        void 조회_실패() {
            given(orderRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.findById(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(OrderErrorCode.ORDER_NOT_FOUND.getMessage())
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);
        }
    }
}
