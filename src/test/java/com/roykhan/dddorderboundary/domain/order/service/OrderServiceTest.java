package com.roykhan.dddorderboundary.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.roykhan.dddorderboundary.common.exception.BusinessException;
import com.roykhan.dddorderboundary.common.exception.OrderErrorCode;
import com.roykhan.dddorderboundary.common.exception.ReservationErrorCode;
import com.roykhan.dddorderboundary.common.exception.StockErrorCode;
import com.roykhan.dddorderboundary.domain.order.Order;
import com.roykhan.dddorderboundary.domain.order.OrderItem;
import com.roykhan.dddorderboundary.domain.order.dto.CreateOrderRequest;
import com.roykhan.dddorderboundary.domain.order.dto.OrderInfo;
import com.roykhan.dddorderboundary.domain.order.enums.OrderStatus;
import com.roykhan.dddorderboundary.domain.order.repository.OrderRepository;
import com.roykhan.dddorderboundary.domain.product.Product;
import com.roykhan.dddorderboundary.domain.product.repository.ProductRepository;
import com.roykhan.dddorderboundary.domain.stock.Stock;
import com.roykhan.dddorderboundary.domain.stock.StockReservation;
import com.roykhan.dddorderboundary.domain.stock.enums.ReservationStatus;
import com.roykhan.dddorderboundary.domain.stock.repository.StockRepository;
import com.roykhan.dddorderboundary.domain.stock.service.StockReservationService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

    private static final int EXPIRE_MINUTES = 10;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StockReservationService stockReservationService;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> expireAtCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "expireTime", EXPIRE_MINUTES);
    }

    private static Product product(Long id, String name, String price) {
        Product product = Product.builder()
            .name(name)
            .description("설명")
            .price(new BigDecimal(price))
            .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private static Stock stock(Long id, Long productId, int quantity) {
        Stock stock = Stock.create(productId, quantity);
        ReflectionTestUtils.setField(stock, "id", id);
        return stock;
    }

    private static CreateOrderRequest request(Long memberId, CreateOrderRequest.OrderLine... lines) {
        return new CreateOrderRequest(memberId, List.of(lines));
    }

    private static CreateOrderRequest.OrderLine line(Long productId, int quantity) {
        return new CreateOrderRequest.OrderLine(productId, quantity);
    }

    // 예약까지 걸린 PENDING 주문. 실제 도메인 객체를 써서 재고 변화까지 함께 확인한다
    private static Order pendingOrder(long orderId, Stock stock, int quantity) {
        return pendingOrder(orderId, stock, quantity, LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
    }

    private static Order pendingOrder(long orderId, Stock stock, int quantity, LocalDateTime expireAt) {
        Order order = Order.create(1L, expireAt);
        order.addItem(stock.getProductId(), "상품", new BigDecimal("1000"), quantity);
        StockReservation.create(stock, order, quantity, expireAt);
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    // 저장 시점에 식별자가 부여되는 JPA 동작을 흉내낸다. createOrder 가 주문 ID 를 반환하므로 필요하다
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
            given(productRepository.findAllById(List.of(1L))).willReturn(List.of(product(1L, "위스키", "1000")));
            given(stockRepository.findAllByProductIdIn(List.of(1L))).willReturn(List.of(stock(10L, 1L, 100)));
            givenOrderSaveAssignsId(42L);

            long orderId = orderService.createOrder(request(7L, line(1L, 2)));

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
            Product whisky = product(1L, "글렌피딕 12년", "89000.00");
            given(productRepository.findAllById(List.of(1L))).willReturn(List.of(whisky));
            given(stockRepository.findAllByProductIdIn(List.of(1L))).willReturn(List.of(stock(10L, 1L, 100)));
            givenOrderSaveAssignsId(1L);

            orderService.createOrder(request(1L, line(1L, 3)));

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
        @DisplayName("주문 후 상품 가격이 바뀌어도 항목에 복사된 단가는 그대로다")
        void 상품_가격_변경과_무관() {
            Product whisky = product(1L, "글렌피딕 12년", "89000.00");
            given(productRepository.findAllById(List.of(1L))).willReturn(List.of(whisky));
            given(stockRepository.findAllByProductIdIn(List.of(1L))).willReturn(List.of(stock(10L, 1L, 100)));
            givenOrderSaveAssignsId(1L);

            orderService.createOrder(request(1L, line(1L, 2)));

            whisky.setPrice(new BigDecimal("50000.00"));

            verify(orderRepository).save(orderCaptor.capture());
            Order saved = orderCaptor.getValue();
            assertThat(saved.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("89000.00");
            assertThat(saved.getTotalPrice()).isEqualByComparingTo("178000.00");
        }

        @Test
        @DisplayName("총 주문 금액을 항목 금액의 합으로 계산한다")
        void 총액_계산() {
            given(productRepository.findAllById(List.of(1L, 2L)))
                .willReturn(List.of(product(1L, "상품1", "1000"), product(2L, "상품2", "250.50")));
            given(stockRepository.findAllByProductIdIn(List.of(1L, 2L)))
                .willReturn(List.of(stock(10L, 1L, 100), stock(20L, 2L, 100)));
            givenOrderSaveAssignsId(1L);

            // 1000 * 2 + 250.50 * 4 = 3002.00
            orderService.createOrder(request(1L, line(1L, 2), line(2L, 4)));

            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getTotalPrice()).isEqualByComparingTo("3002.00");
        }

        @Test
        @DisplayName("항목마다 재고를 예약하고 주문과 같은 만료 시각을 넘긴다")
        void 항목별_재고_예약() {
            Stock first = stock(10L, 1L, 100);
            Stock second = stock(20L, 2L, 100);
            given(productRepository.findAllById(List.of(1L, 2L)))
                .willReturn(List.of(product(1L, "상품1", "1000"), product(2L, "상품2", "2000")));
            given(stockRepository.findAllByProductIdIn(List.of(1L, 2L)))
                .willReturn(List.of(first, second));
            givenOrderSaveAssignsId(1L);

            orderService.createOrder(request(1L, line(1L, 2), line(2L, 3)));

            verify(stockReservationService).reserveStock(any(Order.class), eq(first), eq(2), expireAtCaptor.capture());
            verify(stockReservationService).reserveStock(any(Order.class), eq(second), eq(3), expireAtCaptor.capture());

            verify(orderRepository).save(orderCaptor.capture());
            assertThat(expireAtCaptor.getAllValues()).containsOnly(orderCaptor.getValue().getExpireAt());
        }

        @Test
        @DisplayName("만료 시각을 설정된 분만큼 뒤로 잡는다")
        void 만료_시각() {
            given(productRepository.findAllById(List.of(1L))).willReturn(List.of(product(1L, "상품1", "1000")));
            given(stockRepository.findAllByProductIdIn(List.of(1L))).willReturn(List.of(stock(10L, 1L, 100)));
            givenOrderSaveAssignsId(1L);

            LocalDateTime before = LocalDateTime.now();
            orderService.createOrder(request(1L, line(1L, 1)));
            LocalDateTime after = LocalDateTime.now();

            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getExpireAt())
                .isBetween(before.plusMinutes(EXPIRE_MINUTES), after.plusMinutes(EXPIRE_MINUTES));
        }

        @Test
        @DisplayName("같은 상품을 여러 줄로 주문하면 항목도 줄 수만큼 담고 각각 예약한다")
        void 같은_상품_여러_줄() {
            Stock stock = stock(10L, 1L, 100);
            given(productRepository.findAllById(List.of(1L))).willReturn(List.of(product(1L, "상품1", "1000")));
            given(stockRepository.findAllByProductIdIn(List.of(1L))).willReturn(List.of(stock));
            givenOrderSaveAssignsId(1L);

            orderService.createOrder(request(1L, line(1L, 3), line(1L, 5)));

            verify(orderRepository).save(orderCaptor.capture());
            Order saved = orderCaptor.getValue();
            assertThat(saved.getItems()).hasSize(2);
            assertThat(saved.getItems()).extracting(OrderItem::getQuantity).containsExactly(3, 5);
            assertThat(saved.getTotalPrice()).isEqualByComparingTo("8000");

            verify(stockReservationService).reserveStock(any(Order.class), eq(stock), eq(3), any(LocalDateTime.class));
            verify(stockReservationService).reserveStock(any(Order.class), eq(stock), eq(5), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("항목 목록은 읽기 전용이라 밖에서 담을 수 없다")
        void 항목_목록_읽기_전용() {
            given(productRepository.findAllById(List.of(1L))).willReturn(List.of(product(1L, "상품1", "1000")));
            given(stockRepository.findAllByProductIdIn(List.of(1L))).willReturn(List.of(stock(10L, 1L, 100)));
            givenOrderSaveAssignsId(1L);

            orderService.createOrder(request(1L, line(1L, 1)));

            verify(orderRepository).save(orderCaptor.capture());
            List<OrderItem> items = orderCaptor.getValue().getItems();

            assertThatThrownBy(() -> items.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("없는 상품이 섞여 있으면 주문을 저장하지 않고 INVALID_ORDER_ITEM 으로 실패한다")
        void 상품_없음() {
            given(productRepository.findAllById(List.of(1L, 9999L)))
                .willReturn(List.of(product(1L, "상품1", "1000")));

            assertThatThrownBy(() -> orderService.createOrder(request(1L, line(1L, 1), line(9999L, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessage(OrderErrorCode.INVALID_ORDER_ITEM.getMessage())
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.INVALID_ORDER_ITEM);

            verify(orderRepository, never()).save(any());
            verifyNoInteractions(stockReservationService);
        }

        @Test
        @DisplayName("상품은 있지만 재고 레코드가 없으면 INVALID_ORDER_ITEM 으로 실패한다")
        void 재고_레코드_없음() {
            given(productRepository.findAllById(List.of(1L))).willReturn(List.of(product(1L, "상품1", "1000")));
            given(stockRepository.findAllByProductIdIn(List.of(1L))).willReturn(List.of());

            assertThatThrownBy(() -> orderService.createOrder(request(1L, line(1L, 1))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.INVALID_ORDER_ITEM);

            verify(orderRepository, never()).save(any());
            verifyNoInteractions(stockReservationService);
        }

        @Test
        @DisplayName("재고가 부족하면 예약 단계의 예외가 그대로 전파된다")
        void 재고_부족_전파() {
            Stock stock = stock(10L, 1L, 1);
            given(productRepository.findAllById(List.of(1L))).willReturn(List.of(product(1L, "상품1", "1000")));
            given(stockRepository.findAllByProductIdIn(List.of(1L))).willReturn(List.of(stock));
            givenOrderSaveAssignsId(1L);
            doThrow(StockErrorCode.OUT_OF_STOCK.exception())
                .when(stockReservationService).reserveStock(any(), any(), eq(5), any());

            assertThatThrownBy(() -> orderService.createOrder(request(1L, line(1L, 5))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(StockErrorCode.OUT_OF_STOCK);
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("PENDING 주문을 취소하면 CANCELLED 로 바꾸고 잡아둔 재고를 돌려준다")
        void 취소_성공() {
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3);
            assertThat(stock.getAvailableQuantity()).isEqualTo(7);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            orderService.cancelOrder(1L);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelledAt()).isNotNull();
            assertThat(stock.getAvailableQuantity()).isEqualTo(10);
            assertThat(order.getReservations())
                .allMatch(reservation -> reservation.getReservationStatus() == ReservationStatus.CANCELLED);
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
        }

        @Test
        @DisplayName("이미 취소된 주문은 ORDER_ALREADY_CANCELLED 로 실패하고 재고를 다시 돌려주지 않는다")
        void 이미_취소됨() {
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3);
            ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.CANCELLED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(OrderErrorCode.ORDER_ALREADY_CANCELLED.getMessage())
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_CANCELLED);

            assertThat(stock.getAvailableQuantity()).isEqualTo(7);
        }

        @Test
        @DisplayName("만료된 주문은 ORDER_ALREADY_EXPIRED 로 실패한다")
        void 이미_만료됨() {
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3);
            ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.EXPIRED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(OrderErrorCode.ORDER_ALREADY_EXPIRED.getMessage())
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_EXPIRED);

            assertThat(stock.getAvailableQuantity()).isEqualTo(7);
        }

        @Test
        @DisplayName("확정된 주문은 ORDER_ALREADY_CONFIRMED 로 실패한다 - 환불은 결제 취소가 선행되어야 한다")
        void 이미_확정됨() {
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3);
            ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.CONFIRMED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(OrderErrorCode.ORDER_ALREADY_CONFIRMED.getMessage())
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_CONFIRMED);

            assertThat(stock.getAvailableQuantity()).isEqualTo(7);
        }

        @Test
        @DisplayName("결제에 실패한 주문은 ORDER_PAYMENT_FAILED 로 실패한다 - 재고는 이미 돌려주었다")
        void 결제_실패함() {
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3);
            ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.PAYMENT_FAILED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_PAYMENT_FAILED);

            assertThat(stock.getAvailableQuantity()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("confirmOrder")
    class ConfirmOrder {

        @Test
        @DisplayName("결제에 성공하면 CONFIRMED 로 바꾸고 예약한 수량만큼 총 재고를 차감한다")
        void 확정_성공() {
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            orderService.confirmOrder(1L);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(order.getConfirmedAt()).isNotNull();
            assertThat(order.getCancelledAt()).isNull();

            // 가용 수량은 예약 시점에 이미 빠졌으므로 그대로이고, 총 재고가 따라 내려온다
            assertThat(stock.getQuantity()).isEqualTo(7);
            assertThat(stock.getAvailableQuantity()).isEqualTo(7);
            assertThat(stock.reservedQuantity()).isZero();
            assertThat(order.getReservations())
                .allMatch(reservation -> reservation.getReservationStatus() == ReservationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("주문이 없으면 ORDER_NOT_FOUND 로 실패한다")
        void 주문_없음() {
            given(orderRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.confirmOrder(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 확정된 주문은 ORDER_ALREADY_CONFIRMED 로 실패하고 재고를 다시 차감하지 않는다")
        void 이미_확정됨() {
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3);
            ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.CONFIRMED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.confirmOrder(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_CONFIRMED);

            assertThat(stock.getQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("결제에 실패한 주문은 ORDER_PAYMENT_FAILED 로 실패한다")
        void 결제_실패함() {
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3);
            ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.PAYMENT_FAILED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.confirmOrder(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_PAYMENT_FAILED);

            assertThat(stock.getQuantity()).isEqualTo(10);
        }

        // 만료 스케줄러가 아직 돌지 않아 주문은 PENDING 이지만 예약의 결제 마감은 지난 경우
        @Test
        @DisplayName("결제 마감이 지난 예약은 RESERVATION_EXPIRED 로 실패하고 총 재고를 차감하지 않는다")
        void 결제_마감_지남() {
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3, LocalDateTime.now().minusMinutes(1));
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.confirmOrder(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReservationErrorCode.RESERVATION_EXPIRED);

            assertThat(stock.getQuantity()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("failPayment")
    class FailPayment {

        @Test
        @DisplayName("결제에 실패하면 PAYMENT_FAILED 로 바꾸고 예약한 재고를 돌려준다")
        void 실패_반영_성공() {
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            orderService.failPayment(1L);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
            assertThat(order.getCancelledAt()).isNotNull();
            assertThat(order.getConfirmedAt()).isNull();

            assertThat(stock.getQuantity()).isEqualTo(10);
            assertThat(stock.getAvailableQuantity()).isEqualTo(10);
            assertThat(order.getReservations())
                .allMatch(reservation -> reservation.getReservationStatus() == ReservationStatus.CANCELLED);
        }

        @Test
        @DisplayName("주문이 없으면 ORDER_NOT_FOUND 로 실패한다")
        void 주문_없음() {
            given(orderRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.failPayment(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 확정된 주문은 ORDER_ALREADY_CONFIRMED 로 실패하고 재고를 돌려주지 않는다")
        void 이미_확정됨() {
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3);
            ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.CONFIRMED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.failPayment(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_CONFIRMED);

            assertThat(stock.getAvailableQuantity()).isEqualTo(7);
        }

        @Test
        @DisplayName("결제 마감이 지난 주문이어도 아직 PENDING 이면 재고를 돌려준다")
        void 결제_마감_지남() {
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3, LocalDateTime.now().minusMinutes(1));
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            orderService.failPayment(1L);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
            assertThat(stock.getAvailableQuantity()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("findExpiredOrderIds")
    class FindExpiredOrderIds {

        @Test
        @DisplayName("PENDING 주문 중 기준 시각 이전에 마감된 것을 요청한 수만큼 조회한다")
        void 만료_대상_조회() {
            LocalDateTime now = LocalDateTime.now();
            given(orderRepository.findIdsByStatusAndExpireAtBefore(OrderStatus.PENDING, now, PageRequest.of(0, 100)))
                .willReturn(List.of(3L, 1L));

            List<Long> orderIds = orderService.findExpiredOrderIds(now, 100);

            assertThat(orderIds).containsExactly(3L, 1L);
        }
    }

    @Nested
    @DisplayName("expireOrder")
    class ExpireOrder {

        @Test
        @DisplayName("PENDING 주문을 EXPIRED 로 바꾸고 예약한 재고를 돌려준다")
        void 만료_성공() {
            LocalDateTime expireAt = LocalDateTime.now().minusMinutes(1);
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3, expireAt);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            orderService.expireOrder(1L);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.EXPIRED);
            assertThat(order.getCancelledAt()).isNotNull();
            assertThat(stock.getQuantity()).isEqualTo(10);
            assertThat(stock.getAvailableQuantity()).isEqualTo(10);
            assertThat(order.getReservations())
                .allMatch(reservation -> reservation.getReservationStatus() == ReservationStatus.EXPIRED);
        }

        @Test
        @DisplayName("만료해도 결제 마감 시각은 덮어쓰지 않는다")
        void 마감_시각_유지() {
            LocalDateTime expireAt = LocalDateTime.now().minusMinutes(1);
            Order order = pendingOrder(1L, stock(10L, 1L, 10), 3, expireAt);
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
        }

        // 스케줄러가 ID 를 읽은 뒤 결제가 먼저 끝난 경우
        @Test
        @DisplayName("이미 확정된 주문은 ORDER_ALREADY_CONFIRMED 로 실패하고 재고를 돌려주지 않는다")
        void 이미_확정됨() {
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3, LocalDateTime.now().minusMinutes(1));
            ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.CONFIRMED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.expireOrder(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_CONFIRMED);

            assertThat(stock.getAvailableQuantity()).isEqualTo(7);
        }

        @Test
        @DisplayName("이미 취소된 주문은 ORDER_ALREADY_CANCELLED 로 실패하고 재고를 다시 돌려주지 않는다")
        void 이미_취소됨() {
            Stock stock = stock(10L, 1L, 10);
            Order order = pendingOrder(1L, stock, 3, LocalDateTime.now().minusMinutes(1));
            ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.CANCELLED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.expireOrder(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(OrderErrorCode.ORDER_ALREADY_CANCELLED);

            assertThat(stock.getAvailableQuantity()).isEqualTo(7);
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
